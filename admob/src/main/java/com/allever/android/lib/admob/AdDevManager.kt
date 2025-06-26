package com.allever.android.lib.admob

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

@SuppressLint("StaticFieldLeak")
object AdDevManager {

    var adActivity: Activity? = null

    private val interAdId = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/1033173712"
    } else {
        "ca-app-pub-6971932974635976/8204818021"
    }
    private val rewardAdId = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/5224354917"
    } else {
        "ca-app-pub-6971932974635976/9193647822"
    }
    private val nativeAdId = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/2247696110"
    } else {
        "ca-app-pub-6971932974635976/7880566153"
    }

    fun init(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
                log("onActivityResumed: ${activity.javaClass.name}")
                //inter -> com.google.android.gms.ads.AdActivity
                //reward -> com.google.android.gms.ads.AdActivity
                if (activity.javaClass.name.contains("com.google.android.gms.ads")) {
                    adActivity = activity
                }
            }

            override fun onActivityPaused(activity: Activity) {
                log("onActivityPaused: ${activity.javaClass.name}")
            }

            override fun onActivityStopped(activity: Activity) {
                log("onActivityStopped: ${activity.javaClass.name}")
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityDestroyed(activity: Activity) {
                log("onActivityDestroyed: ${activity.javaClass.name}")
                if (activity.javaClass.name.contains("com.google.android.gms.ads")) {
                    adActivity = null
                }
            }
        })
    }

    fun loadDevInter(activity: Activity, adCallback: AdCallback?) {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            activity,
            interAdId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adCallback?.onAdFailLoad()
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdClicked() {
                            adCallback?.onAdClick()
                        }

                        override fun onAdShowedFullScreenContent() {
                            adCallback?.onAdShow()
                        }
                    }
                    interstitialAd.show(activity)
                }
            })
    }

    fun loadDevNative(viewGroup: ViewGroup, adCallback: AdCallback?) {
        val adLoader = AdLoader.Builder(viewGroup.context, nativeAdId)
            .forNativeAd {
                val adView = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.ad_native_small, null) as NativeAdView
                viewGroup.removeAllViews()
                setNativeAdViewContent(it, adView)
                viewGroup.addView(adView)
            }
            .withAdListener(object : AdListener() {
                override fun onAdLoaded() {
                    log("nativeBanner加载成功")
                    adCallback?.onAdLoaded()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    logE("nativeBanner加载失败${error.code} -> ${error.message}")
                    adCallback?.onAdFailLoad()
                }

                override fun onAdClicked() {
                    adCallback?.onAdClick()
                }

                override fun onAdImpression() {
                    adCallback?.onAdShow()
                }
            })
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun destroyDevNative(viewGroup: ViewGroup) {
        val count = viewGroup.childCount
        for (i in 0 until count) {
            val it = viewGroup.getChildAt(i)
            if (it is NativeAdView) {
                it.destroy()
            }
        }
        viewGroup.removeAllViews()
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

    fun loadDevReward(
        activity: Activity,
        adCallback: AdCallback?
    ) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(activity, rewardAdId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                adCallback?.onAdLoaded()
                ad.setOnPaidEventListener {
                    log("loadReward: onPaidEvent")
                }
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdClicked() {
                        log("loadReward: onAdClicked")
                        adCallback?.onAdClick()
                    }

                    override fun onAdDismissedFullScreenContent() {
                        log("loadReward: onAdDismissedFullScreenContent")
                    }

                    override fun onAdFailedToShowFullScreenContent(aderror: AdError) {
                        log("loadReward: onAdFailedToShowFullScreenContent: ${aderror.message}")
                    }

                    override fun onAdImpression() {
                        log("loadReward: onAdImpression")
                        adCallback?.onAdShow()
                    }

                    override fun onAdShowedFullScreenContent() {
                        log("loadReward: onAdShowedFullScreenContent")
                    }
                }

                ad.show(activity) {
                }

            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                log("loadReward: onAdFailedToLoad -> ${adError.code}: ${adError.message}")
                adCallback?.onAdFailLoad()
            }
        })
    }
}