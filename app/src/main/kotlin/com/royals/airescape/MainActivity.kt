package com.royals.airescape

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import com.royals.airescape.ads.AdManager
import com.royals.airescape.data.GameData
import com.royals.airescape.engine.GameSurfaceView
import com.royals.airescape.engine.PlayGamesManager
import com.royals.airescape.engine.RatingManager

class MainActivity : Activity() {
    private lateinit var gameSurfaceView: GameSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GameData.init(this)

        // FrameLayout to hold SurfaceView + banner ad overlay
        val rootLayout = FrameLayout(this)
        gameSurfaceView = GameSurfaceView(this)
        rootLayout.addView(gameSurfaceView)

        setContentView(rootLayout)

        val density = resources.displayMetrics.density
        GameSurfaceView.safeInsetTop = 44f * density
        GameSurfaceView.safeInsetBottom = 48f * density
        GameSurfaceView.bannerHeight = 50f * density

        rootLayout.setOnApplyWindowInsetsListener { _, insets ->
            val topInset: Int
            val bottomInset: Int
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val sysInsets = insets.getInsets(
                    WindowInsets.Type.statusBars() or
                    WindowInsets.Type.displayCutout() or
                    WindowInsets.Type.navigationBars()
                )
                topInset = sysInsets.top
                bottomInset = sysInsets.bottom
            } else {
                @Suppress("DEPRECATION")
                topInset = insets.systemWindowInsetTop
                @Suppress("DEPRECATION")
                bottomInset = insets.systemWindowInsetBottom
            }
            GameSurfaceView.safeInsetTop = topInset.toFloat().coerceAtLeast(44f * density)
            GameSurfaceView.safeInsetBottom = bottomInset.toFloat().coerceAtLeast(48f * density)
            AdManager.updateBannerBottomMargin()
            insets
        }

        // Initialize AdMob and create banner
        AdManager.init(this)
        AdManager.createBanner(rootLayout)

        // Initialize Google Play Games
        PlayGamesManager.init(this)

        // Initialize In-App Review
        RatingManager.init(this)

        hideSystemUI()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        gameSurfaceView.resume()
        gameSurfaceView.resumeScreen()
    }

    override fun onPause() {
        super.onPause()
        gameSurfaceView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        AdManager.destroy()
        PlayGamesManager.release()
        RatingManager.release()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (gameSurfaceView.currentScreen?.onBackPressed() == true) {
            return
        }
        super.onBackPressed()
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }
}
