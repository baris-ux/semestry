package com.example.semestry.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object RewardedAdManager {

    private const val AD_UNIT_ID = "ca-app-pub-5635129405516539/3350850979"

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    fun load(context: Context) {
        if (rewardedAd != null || isLoading) return
        isLoading = true
        RewardedAd.load(
            context.applicationContext,
            AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                }
            }
        )
    }

    fun isReady() = rewardedAd != null

    fun show(activity: Activity, onRewarded: () -> Unit, onDismissed: () -> Unit) {
        val ad = rewardedAd ?: run { onDismissed(); return }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                load(activity)
                onDismissed()
            }
        }
        ad.show(activity) { onRewarded() }
    }
}
