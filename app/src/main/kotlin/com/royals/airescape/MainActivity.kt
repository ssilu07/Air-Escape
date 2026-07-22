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
