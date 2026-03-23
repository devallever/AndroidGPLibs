package com.allever.android.lib.admob

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 *@Description
 *@author: Allever
 *@date: 2024/1/4
 */
object GDPRHelper {
    private val TAG = GDPRHelper::class.java.simpleName
    private lateinit var mConsentInformation: ConsentInformation

    fun init(context: Context) {
        mConsentInformation = UserMessagingPlatform.getConsentInformation(context)
    }

    fun requestConsentInfoUpdate(activity: Activity, success: () -> Unit) {
        logE("requestConsentInfoUpdate: ")
        val debugSettings = if (BuildConfig.DEBUG) {
            ConsentDebugSettings.Builder(activity.applicationContext)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .addTestDeviceHashedId("FCC50892966215F27882878753C9AEE4")
                .build()
        } else {
            null
        }

        // Create a ConsentRequestParameters object.
        val params = ConsentRequestParameters
            .Builder()
            .setConsentDebugSettings(debugSettings)
            .build()

        mConsentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                log("OnConsentInfoUpdateSuccessListener: success!!")
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    activity
                ) { loadAndShowError ->
                    loadAndShowError?.let {
                        // Consent gathering failed.
                        log("loadAndShowError: Consent gathering failed: ${loadAndShowError.errorCode}, ${loadAndShowError.message}")
                    }
                    // Consent has been gathered.
                    success.invoke()

                }
            },
            { requestConsentError ->
                // Consent gathering failed.
                logE("requestConsentError Consent gathering failed: ${requestConsentError.errorCode}, ${requestConsentError.message}")
            })

    }

    fun canRequestAds(): Boolean {
        val result = mConsentInformation.canRequestAds()
        if (result) {
            log("consentInformation.canRequestAds() = true")
        } else {
            logE("consentInformation.canRequestAds() = false")
        }
        return result
    }

    private fun log(msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, msg)
        }
    }

    private fun logE(msg: String) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, msg)
        }
    }
}