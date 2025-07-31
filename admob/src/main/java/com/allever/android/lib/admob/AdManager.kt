package com.allever.android.lib.admob

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

object AdManager {

    private var mAdConfig: IAdConfig = TestAdConfig()
    private lateinit var mContext: Application
    private var mInterAdCache: InterstitialAd? = null
    private var mInterAdCacheTime = 0L
    private const val CACHE_TIME_OUT =  45 * 60 * 1000L

    fun init(context: Application) {
        mContext = context
    }

    fun init(adConfig: IAdConfig, context: Application) {
        mAdConfig = adConfig
        mContext = context
        MobileAds.initialize(context) {
            log("MobileAds: 初始化成功")
            justLoadInter()
        }
    }

    fun justLoadInter() {
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

    fun showInter(activity: Activity, next: () -> Unit) {

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
        mInterAdCache?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                log("InterAdCache: 关闭")
                mInterAdCache = null
                next()
                justLoadInter()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                log("InterAdCache: 显示失败")
                mInterAdCache = null
                next()
                justLoadInter()
            }
        }
        mInterAdCache?.show(activity)
    }

    fun loadBanner(bannerContainer: ViewGroup): AdView {
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