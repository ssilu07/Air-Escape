package com.royals.airescape.input

import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import com.royals.airescape.util.Vector2

/**
 * Touch-to-move controller.
 *
 * The player touches anywhere on the screen and the character moves toward that
 * point.  A small crosshair is drawn at the touch location while the finger is
 * down.  [setPlayerPosition] must be called each frame so [getDirection] can
 * compute the vector from the player to the touch target.
 */
class TouchController : InputManager {

    // ── state ───────────────────────────────────────────────────────────────
    private var touchPosition: Vector2? = null

    // ── screen dimensions ───────────────────────────────────────────────────
    private var screenWidth = 0f
    private var screenHeight = 0f

    // ── threshold below which the character is "close enough" ───────────────
    private val arrivalThreshold = 20f

    // ── crosshair size ──────────────────────────────────────────────────────
    private val crosshairSize = 14f

    // ── paints (allocated once) ─────────────────────────────────────────────
    private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
        alpha = 180
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF4444.toInt()
        style = Paint.Style.FILL
        alpha = 200
    }

    // ── InputManager ────────────────────────────────────────────────────────

    override fun setScreenSize(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                touchPosition = Vector2(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchPosition = null
                return true
            }
        }
        return false
    }

    override fun getDirection(): Vector2 {
        val target = touchPosition ?: return Vector2.ZERO
        val center = Vector2(screenWidth / 2f, screenHeight / 2f)
        val delta = target - center
        return if (delta.magnitude() > arrivalThreshold) delta.normalized() else Vector2.ZERO
    }

    override fun render(canvas: Canvas) {
        val pos = touchPosition ?: return

        // Centre dot
        canvas.drawCircle(pos.x, pos.y, 3f, dotPaint)

        // Crosshair lines
        canvas.drawLine(
            pos.x - crosshairSize, pos.y,
            pos.x + crosshairSize, pos.y,
            crosshairPaint
        )
        canvas.drawLine(
            pos.x, pos.y - crosshairSize,
            pos.x, pos.y + crosshairSize,
            crosshairPaint
        )

        // Outer ring
        canvas.drawCircle(pos.x, pos.y, crosshairSize, crosshairPaint)
    }

}
