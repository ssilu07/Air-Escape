package com.royals.airescape.ads

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import com.royals.airescape.BuildConfig
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Manages all AdMob ads: Banner, Interstitial, and Rewarded.
 * Ad IDs are picked automatically from BuildConfig (debug = test, release = real).
 */
object AdManager {

    // ── Ad Unit IDs (from build variants) ─────────────────────────────
    private val BANNER_ID = BuildConfig.BANNER_ID
    private val INTERSTITIAL_ID = BuildConfig.INTERSTITIAL_ID
    private val REWARDED_ID = BuildConfig.REWARDED_ID

    // ── State ────────────────────────────────────────────────────────
    private var activity: Activity? = null
    private var bannerAdView: AdView? = null
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    /** Tracks game-over count for interstitial frequency. */
    var gameOverCount: Int = 0

    // ── Init ─────────────────────────────────────────────────────────

    fun init(activity: Activity) {
        this.activity = activity
        MobileAds.initialize(activity) {}
        loadInterstitial()
        loadRewarded()
    }

    // ── Banner ───────────────────────────────────────────────────────

    val isBannerVisible: Boolean
        get() = bannerAdView?.visibility == View.VISIBLE

    fun createBanner(container: FrameLayout): AdView {
        val adView = AdView(container.context)
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = BANNER_ID
        bannerAdView = adView

        val density = container.context.resources.displayMetrics.density
        com.royals.airescape.engine.GameSurfaceView.bannerHeight = 50f * density

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
            bottomMargin = com.royals.airescape.engine.GameSurfaceView.safeInsetBottom.toInt()
        }
        container.addView(adView, params)
        adView.loadAd(AdRequest.Builder().build())
        adView.visibility = View.GONE
        return adView
    }

    fun updateBannerBottomMargin() {
        val view = bannerAdView ?: return
        activity?.runOnUiThread {
            val params = view.layoutParams as? FrameLayout.LayoutParams ?: return@runOnUiThread
            params.bottomMargin = com.royals.airescape.engine.GameSurfaceView.safeInsetBottom.toInt()
            view.layoutParams = params
        }
    }

    fun showBanner() {
        activity?.runOnUiThread { bannerAdView?.visibility = View.VISIBLE }
    }

    fun hideBanner() {
        activity?.runOnUiThread { bannerAdView?.visibility = View.GONE }
    }

    // ── Interstitial ─────────────────────────────────────────────────

    private fun loadInterstitial() {
        val act = activity ?: return
        InterstitialAd.load(
            act,
            INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    /** Show interstitial if loaded. Auto-reloads after dismiss. */
    fun showInterstitial(onDismissed: (() -> Unit)? = null) {
        val act = activity ?: return
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitial()
                    onDismissed?.invoke()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    loadInterstitial()
                    onDismissed?.invoke()
                }
            }
            act.runOnUiThread { ad.show(act) }
        } else {
            loadInterstitial()
            onDismissed?.invoke()
        }
    }

    fun isInterstitialReady(): Boolean = interstitialAd != null

    // ── Rewarded ─────────────────────────────────────────────────────

    private fun loadRewarded() {
        val act = activity ?: return
        RewardedAd.load(
            act,
            REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                }
            }
        )
    }

    /** Show rewarded ad. [onRewarded] called if user earns reward. */
    fun showRewarded(onRewarded: () -> Unit, onDismissed: (() -> Unit)? = null) {
        val act = activity ?: return
        val ad = rewardedAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewarded()
                    onDismissed?.invoke()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    rewardedAd = null
                    loadRewarded()
                    onDismissed?.invoke()
                }
            }
            act.runOnUiThread {
                ad.show(act) { onRewarded() }
            }
        } else {
            loadRewarded()
        }
    }

    fun isRewardedReady(): Boolean = rewardedAd != null

    // ── Cleanup ──────────────────────────────────────────────────────

    fun destroy() {
        bannerAdView?.destroy()
        bannerAdView = null
        activity = null
    }
}
