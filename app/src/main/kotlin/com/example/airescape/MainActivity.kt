package com.example.airescape

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import com.example.airescape.engine.GameSurfaceView
import com.example.airescape.screen.MenuScreen
import com.example.airescape.data.GameData

class MainActivity : Activity() {
    private lateinit var gameSurfaceView: GameSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GameData.init(this)
        gameSurfaceView = GameSurfaceView(this)
        setContentView(gameSurfaceView)
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
