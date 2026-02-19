package com.example.airescape

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import com.example.airescape.ads.AdManager
import com.example.airescape.data.GameData
import com.example.airescape.engine.GameSurfaceView

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
