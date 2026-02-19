package com.royals.airescape.engine

import android.content.Context
import android.graphics.Canvas
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.royals.airescape.data.Constants
import com.royals.airescape.screen.Screen
import com.royals.airescape.screen.MenuScreen

/**
 * Custom SurfaceView that hosts the game loop and delegates
 * update / render / touch to the currently active [Screen].
 */
class GameSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, GameLoop.Callback {

    private var gameLoop: GameLoop? = null
    var currentScreen: Screen? = null
        private set

    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
    }

    companion object {
        @Volatile
        var screenWidth: Int = 0
            private set

        @Volatile
        var screenHeight: Int = 0
            private set
    }

    // ---- Public API ----

    fun setScreen(screen: Screen) {
        currentScreen?.onExit()
        currentScreen = screen
        screen.onEnter()
    }

    fun resume() {
        if ((gameLoop == null || !gameLoop!!.running) && holder.surface.isValid) {
            gameLoop = GameLoop(this).also { it.startLoop() }
        }
    }

    fun pause() {
        currentScreen?.onExit()
        gameLoop?.stopLoop()
        gameLoop = null
    }

    fun resumeScreen() {
        currentScreen?.onEnter()
    }

    // ---- SurfaceHolder.Callback ----

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (currentScreen == null) {
            currentScreen = MenuScreen(this)
        }
        gameLoop = GameLoop(this).also { it.startLoop() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        gameLoop?.stopLoop()
        gameLoop = null
    }

    // ---- GameLoop.Callback ----

    override fun update(dt: Float) {
        currentScreen?.update(dt)
    }

    override fun render() {
        val canvas: Canvas?
        try {
            canvas = holder.lockCanvas()
        } catch (_: Exception) {
            return
        }
        if (canvas == null) return

        try {
            canvas.drawColor(Constants.BACKGROUND_COLOR.toInt())
            currentScreen?.render(canvas)
        } finally {
            try {
                holder.unlockCanvasAndPost(canvas)
            } catch (_: Exception) {
                // Surface may have been destroyed between lock and unlock
            }
        }
    }

    // ---- Touch ----

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return currentScreen?.onTouchEvent(event) ?: super.onTouchEvent(event)
    }

    // ---- Back key ----

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (currentScreen?.onBackPressed() == true) {
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
