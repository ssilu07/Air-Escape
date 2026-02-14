package com.example.airescape.screen

import android.graphics.Canvas
import android.view.MotionEvent

/**
 * Contract for all game screens (menu, gameplay, game-over, etc.).
 */
interface Screen {
    fun update(dt: Float)
    fun render(canvas: Canvas)
    fun onTouchEvent(event: MotionEvent): Boolean
    fun onEnter() {}
    fun onExit() {}
    fun onSizeChanged(width: Float, height: Float) {}
    /** Return true if the screen handled the back press. */
    fun onBackPressed(): Boolean = false
}
