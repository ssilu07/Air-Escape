package com.example.airescape.input

import android.graphics.Canvas
import android.view.MotionEvent
import com.example.airescape.util.Vector2

enum class ControlType {
    JOYSTICK, TOUCH, BUTTONS
}

interface InputManager {
    fun onTouchEvent(event: MotionEvent): Boolean
    fun getDirection(): Vector2
    fun render(canvas: Canvas)
    fun setScreenSize(width: Float, height: Float)
}
