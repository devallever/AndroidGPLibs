package com.allever.android.lib.admob

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.lang.ref.WeakReference

object AdManager {

    private var mAdConfig: IAdConfig = TestAdConfig()
    private lateinit var mContext: Application
    private var mInterAdCache: InterstitialAd? = null
    private var mInterAdCacheTime = 0L
    private const val CACHE_TIME_OUT =  45 * 60 * 1000L

    private var skipInterAd = false

    /** ====== 插页广告频率控制 (AdMob 政策合规) ====== */

    /** 两次插页广告之间的最小间隔（毫秒）— 60 秒 */
    private const val INTER_INTERVAL_MS = 60_000L

    /** 单个 Session 内插页广告展示上限 */
    private const val MAX_INTER_PER_SESSION = 4

    /** 上次插页广告展示的时间戳 */
    private var mLastInterShowTime = 0L

    /** 当前 Session 内插页广告已展示次数 */
    private var mSessionInterShowCount = 0

    /**
     * 重置频率控制状态（在 App 从后台恢复时调用，开启新 Session）
     */
    fun resetInterFreqControl() {
        mLastInterShowTime = 0L
        mSessionInterShowCount = 0
    }

    /**
     * 检查当前是否允许展示插页广告
     * @param skipFreqCheck 是否跳过频率检查（特殊场景，默认 false）
     */
    private fun canShowInter(skipFreqCheck: Boolean = false): Boolean {
        if (skipFreqCheck) return true

        val now = System.currentTimeMillis()
        val interval = now - mLastInterShowTime

        // 检查间隔
        if (mLastInterShowTime > 0 && interval < INTER_INTERVAL_MS) {
            val remaining = (INTER_INTERVAL_MS - interval) / 1000
            log("InterFreq: 距上次展示不足 60 秒，剩余 ${remaining}s，跳过")
            return false
        }

        // 检查 Session 上限
//        if (mSessionInterShowCount >= MAX_INTER_PER_SESSION) {
//            log("InterFreq: 当前 Session 已展示 ${mSessionInterShowCount} 次，达到上限 $MAX_INTER_PER_SESSION，跳过")
//            return false
//        }

        return true
    }

    fun init(context: Application, skipInterAd: Boolean = false) {
        log("init")
        mContext = context
        this.skipInterAd = skipInterAd
    }

    fun init(adConfig: IAdConfig, context: Application, block: (() -> Unit)? = null, skipInterAd: Boolean = false) {
        log("init with config")
        mAdConfig = adConfig
        mContext = context
        this.skipInterAd = skipInterAd
        MobileAds.initialize(context) {
            log("MobileAds: 初始化成功")
            justLoadInter()
            block?.invoke()
        }
    }

    fun loadRewardedAd(
        activity: Activity,
        adCallback: AdCallback? = null
    ) {
        val adId = mAdConfig.getAdId(IAdConfig.REWARD_AD)
        log("Loading rewarded ad: $adId")

        val adRequest = AdRequest.Builder().build()
        var rewarded = false

        RewardedAd.load(activity, adId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                logE("rewardedAd: 加载失败 -> ${adError.code}: ${adError.message}")
                adCallback?.onAdFailLoad()
            }

            override fun onAdLoaded(ad: RewardedAd) {
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        log("rewardedAd: 关闭, rewarded=$rewarded")
                        adCallback?.onAdDismiss(rewarded)
                    }

                    override fun onAdShowedFullScreenContent() {
                        log("rewardedAd: 展示")
                        adCallback?.onAdShow()
                    }

                    override fun onAdClicked() {
                        log("rewardedAd: 点击")
                        adCallback?.onAdClick()
                    }

                    override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                        logE("rewardedAd: 展示失败 -> ${p0.code}: ${p0.message}")
                        adCallback?.onAdFailLoad()
                    }
                }

                // 广告已就绪，先通知外部（关闭 loading），再展示
                adCallback?.onAdLoaded()
                ad.show(activity) {
                    log("rewardedAd: 获得奖励")
                    rewarded = true
                }
            }
        })
    }

    /** ====== 今日免广告体系 ====== */

    private const val SP_NAME = "ad_manager_sp"
    private const val SP_KEY_NO_ADS_DATE = "sp_key_no_ads_date"

    private var mSp: SharedPreferences? = null

    /** 延迟获取 SharedPreferences，确保 init() 之后有 mContext */
    private fun getSp(): SharedPreferences {
        if (mSp == null) {
            mSp = mContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        }
        return mSp!!
    }

    /**
     * 获取今日日期 key，格式 yyyy-MM-dd，例如 "2026-10-04"
     */
    private fun todayKey(): String {
        val cal = Calendar.getInstance()
        val y = cal.get(Calendar.YEAR)
        val m = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val d = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        return "$y-$m-$d"
    }

    /**
     * 检查今天是否处于免广告期间
     */
    private fun isNoAdsActive(): Boolean {
        val stored = getSp().getString(SP_KEY_NO_ADS_DATE, "") ?: ""
        val active = stored == todayKey()
        log("免广告检查: stored=$stored, today=${todayKey()}, active=$active")
        return active
    }

    /**
     * 发放奖励：将今日日期写入 SP
     */
    private fun grantNoAdsToday() {
        val today = todayKey()
        getSp().edit().putString(SP_KEY_NO_ADS_DATE, today).apply()
        log("免广告已开启，日期: $today")
    }

    /**
     * 对外主入口：弹"今日免广告"提示弹窗
     *
     * 流程：
     *  ① 今日已在免广告期 → 提示后 callback(false)
     *  ② 弹 AlertDialog（免除 / 暂不）
     *  ③ 点"免除" → 弹 Loading → 调用 loadRewardedAd()
     *  ④ 广告关闭时根据 rewarded 参数决定是否发放奖励
     *
     * @param activity 当前 Activity
     * @param callback 最终结果回调：true=看完完整广告并已免除今日广告
     */
    fun showNoAdsDialog(
        activity: Activity,
        callback: (success: Boolean) -> Unit
    ) {
        // Activity 状态检查
        if (activity.isFinishing || activity.isDestroyed) {
            callback(false)
            return
        }

        // 激励广告始终可看，每次看完都会更新今日免广告日期
        AlertDialog.Builder(activity)
            .setTitle("No Ads Today")
            .setMessage("Watch an ad to remove interstitial, native and banner ads for the rest of today")
            .setPositiveButton("Watch Ad") { dialog, _ ->
                dialog.dismiss()
                showLoadingAndLoadReward(activity, callback)
            }
            .setNegativeButton("Not Now") { dialog, _ ->
                dialog.dismiss()
                callback(false)
            }
            .setCancelable(true)
            .setOnCancelListener { callback(false) }
            .create()
            .show()
    }

    /**
     * 内部流程：弹 Loading → 调用 loadRewardedAd → 处理回调
     */
    private fun showLoadingAndLoadReward(
        activity: Activity,
        callback: (success: Boolean) -> Unit
    ) {
        // Activity 状态检查
        if (activity.isFinishing || activity.isDestroyed) {
            callback(false)
            return
        }

        val loading = ProgressDialog(activity).apply {
            setMessage("Loading ad...")
            setCancelable(false)
            show()
        }

        loadRewardedAd(activity, object : AdCallback {
            override fun onAdLoaded() {
                // loadRewardedAd 内部已调用 ad.show()，广告已显示，关闭 loading
                loading.dismiss()
            }

            override fun onAdFailLoad() {
                loading.dismiss()
                toast(activity, "Failed to load ad. Please try again later.")
                callback(false)
            }

            override fun onAdDismiss(rewarded: Boolean) {
                if (rewarded) {
                    grantNoAdsToday()
                    toast(activity, "Ads removed for today")
                    callback(true)
                } else {
                    toast(activity, "You must watch the full ad to get the reward")
                    callback(false)
                }
            }
        })
    }

    private fun toast(context: Context, msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun justLoadInter() {
        if (skipInterAd) {
            return
        }
        val cacheTime = System.currentTimeMillis() - mInterAdCacheTime
        if (mInterAdCache != null && cacheTime < CACHE_TIME_OUT) {
            return
        }

        mInterAdCache = null

        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            mContext,
            mAdConfig.getAdId(IAdConfig.INTER_AD),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    logE("interAd: 加载失败 -> ${adError.code}")
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    log("interAd: 加载成功")
                    mInterAdCache = interstitialAd
                    mInterAdCacheTime = System.currentTimeMillis()
                    log("interAd: 缓存成功")
                }
            })
    }

    fun canShowInterAd() : Boolean {
        return mInterAdCache != null && !skipInterAd && canShowInter(false)
    }

    /**
     * 展示插页广告
     * @param activity 当前 Activity
     * @param skipAd 是否跳过广告（业务层控制，如订阅用户免广告）
     * @param next 广告关闭或跳过后的回调
     * @param skipFreqCheck 是否跳过频率控制（默认 false，一般不需要）
     */
    fun showInter(
        activity: Activity,
        skipAd: Boolean = false,
        skipFreqCheck: Boolean = false,
        next: () -> Unit
    ) {
        // 业务层跳过
        if (skipAd) {
            next.invoke()
            return
        }

        // 今日免广告拦截（外部零改动，自动生效）
        if (isNoAdsActive()) {
            log("免广告期：跳过插页广告")
            next.invoke()
            return
        }

        // 频率控制检查（AdMob 政策合规）
        if (!canShowInter(skipFreqCheck)) {
            log("InterFreq: 频率控制拦截，跳过插页广告")
            next.invoke()
            return
        }

        // Activity 状态检查
        if (activity.isFinishing || activity.isDestroyed) {
            log("InterAdCache: Activity 已销毁，跳过展示")
            next.invoke()
            return
        }

        // 缓存为空 → 加载并直接执行 next
        if (mInterAdCache == null) {
            justLoadInter()
            log("InterAdCache: 缓存中无广告, 加载广告")
            next.invoke()
            return
        }

        val cacheTime = System.currentTimeMillis() - mInterAdCacheTime
        if (cacheTime > CACHE_TIME_OUT) {
            log("InterAdCache: 缓存已过期，加载广告")
            mInterAdCache = null
            justLoadInter()
            next.invoke()
            return
        }

        log("使用InterAdCache")

        // 记录本次展示时间和次数
        mLastInterShowTime = System.currentTimeMillis()
        mSessionInterShowCount++
        log("InterFreq: 已展示，当前 Session 第 ${mSessionInterShowCount} 次")

        mInterAdCache?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                log("InterAdCache: 关闭")
                mInterAdCache = null
                next()
                justLoadInter()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                log("InterAdCache: 显示失败")
                // 展示失败时不累计次数，回滚
                mSessionInterShowCount = (mSessionInterShowCount - 1).coerceAtLeast(0)
                mInterAdCache = null
                next()
                justLoadInter()
            }
        }
        mInterAdCache?.show(activity)
    }

    fun loadBanner(bannerContainer: ViewGroup): AdView? {
        // 今日免广告拦截
        if (isNoAdsActive()) {
            log("免广告期：跳过 Banner 广告加载")
            return null
        }

        val mBannerAd = AdView(bannerContainer.context)
        val autoAdWidth = getScreenWidth(bannerContainer.context)
        mBannerAd.setAdSize(
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                bannerContainer.context,
                autoAdWidth
            )
        )
        mBannerAd.adUnitId = mAdConfig.getAdId(IAdConfig.BANNER_AD)

        mBannerAd.adListener = object : AdListener() {
            override fun onAdLoaded() {
                bannerContainer.addView(mBannerAd)
                log("加载成功")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                logE("加载失败：${error.code} -> ${error.message}")
            }
        }

        val adRequest = AdRequest.Builder().build()
        mBannerAd.loadAd(adRequest)
        return mBannerAd
    }

    fun resumeBanner(viewGroup: ViewGroup) {
        for (i in 0 until  viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is AdView) {
                child.resume()
            }
        }
    }

    fun pauseBanner(viewGroup: ViewGroup) {
        for (i in 0 until  viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is AdView) {
                child.pause()
            }
        }
    }

    fun destroyBanner(viewGroup: ViewGroup) {
        for (i in 0 until  viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is AdView) {
                child.destroy()
            }
        }
        viewGroup.removeAllViews()
    }

    fun loadInter(block: (interstitialAd: InterstitialAd) -> Unit) {
        if (skipInterAd) {
            return
        }
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            mContext,
            mAdConfig.getAdId(IAdConfig.INTER_AD),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    logE("interAd: 加载失败 -> ${adError.code}")
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    log("interAd: 加载成功")
                    block.invoke(interstitialAd)
                }
            })
    }

    private var mNativeBannerCache = mutableMapOf<String, NativeAd>()
    private var mNativeBannerGroup = mutableMapOf<String, ViewGroup>()
    fun loadNativeAd(
        viewGroup: ViewGroup,
        page: String,
        adLayoutId: Int = R.layout.ad_native_small,
        show: Boolean = true
    ) {
        // 今日免广告拦截（外部零改动，自动生效）
        if (isNoAdsActive()) {
            log("免广告期：跳过原生广告加载")
            destroyNativeAd(page)
            return
        }

        destroyNativeAd(page)
        mNativeBannerGroup[page] = viewGroup
        val adLoader = AdLoader.Builder(viewGroup.context, mAdConfig.getAdId(IAdConfig.NATIVE_AD))
            .forNativeAd {
                log("forNativeAd")
                mNativeBannerCache[page] = it
                val adView = LayoutInflater.from(viewGroup.context)
                    .inflate(adLayoutId, null) as NativeAdView
                viewGroup.removeAllViews()
                setNativeAdViewContent(it, adView)
                viewGroup.addView(adView)
            }
            .withAdListener(object : AdListener() {
                override fun onAdLoaded() {
                    log("nativeBanner加载成功")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    logE("nativeBanner加载失败${error.code} -> ${error.message}")
                }
            })
            .build()
        adLoader.loadAd(AdRequest.Builder().build())

    }

    fun resumeNativeBanner(page: String) {
        destroyNativeAd(page)
        mNativeBannerGroup[page]?.let {
            loadNativeAd(it, page)
        }
    }

    fun destroyNativeAd(page: String) {
        mNativeBannerCache.remove(page)?.destroy()
    }

    private fun setNativeAdViewContent(nativeAd: NativeAd, adNativeView: NativeAdView) {
        val adBody = adNativeView.findViewById<TextView>(R.id.ad_body)
        val adHeadline = adNativeView.findViewById<TextView>(R.id.ad_headline)
        val adIcon = adNativeView.findViewById<ImageView>(R.id.ad_icon)
        val adCta = adNativeView.findViewById<Button>(R.id.ad_cta)
        val adMedia = adNativeView.findViewById<MediaView>(R.id.ad_media)
        val adStore = adNativeView.findViewById<TextView>(R.id.ad_store)
        val adPrice = adNativeView.findViewById<TextView>(R.id.ad_price)
        adNativeView.bodyView = adBody
        adNativeView.iconView = adIcon
        adNativeView.headlineView = adHeadline
        adNativeView.callToActionView = adCta
        adNativeView.mediaView = adMedia
        adNativeView.storeView = adStore
        adNativeView.priceView = adPrice
        adBody.text = nativeAd.body
        adHeadline.text = nativeAd.headline
        val activity = (adIcon?.context as? Activity)
        if (activity?.isFinishing == true || activity?.isDestroyed == true) {
            return
        }
        Glide.with(adIcon!!).load(nativeAd.icon?.drawable)
            .transform(CenterInside(), RoundedCorners(8)).into(adIcon)
        adCta?.text = nativeAd.callToAction
        nativeAd.mediaContent?.let {
            adMedia?.setMediaContent(it)
        }
        adStore?.text = nativeAd.store
        adPrice?.text = nativeAd.price
        adNativeView.setNativeAd(nativeAd)
    }

    private fun log(msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d("ILogger", msg)
        }
    }
    private fun logE(msg: String) {
        if (BuildConfig.DEBUG) {
            Log.e("ILogger", msg)
        }
    }

    private fun getScreenWidth(context: Context): Int {
        val display =
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val outMetrics = DisplayMetrics()
        display?.getMetrics(outMetrics)
        val density = outMetrics.density
        return (outMetrics.widthPixels / density).toInt()
    }
}