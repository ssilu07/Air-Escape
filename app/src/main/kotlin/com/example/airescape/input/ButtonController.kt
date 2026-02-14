package com.example.airescape.input

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import com.example.airescape.util.Vector2
import kotlin.math.min

/**
 * D-pad button controller.
 *
 * Four directional buttons are laid out in a cross pattern at the bottom of the
 * screen: left/right on the bottom-left, up/down on the bottom-right.  The
 * controller supports multi-touch so diagonal movement is possible.
 */
class ButtonController : InputManager {

    // ── button state ────────────────────────────────────────────────────────
    private var leftPressed = false
    private var rightPressed = false
    private var upPressed = false
    private var downPressed = false

    // ── screen dimensions ───────────────────────────────────────────────────
    private var screenWidth = 0f
    private var screenHeight = 0f

    // ── button regions (computed in setScreenSize) ──────────────────────────
    private var leftRect = RectF()
    private var rightRect = RectF()
    private var upRect = RectF()
    private var downRect = RectF()

    private var buttonSize = 70f

    // ── paints ──────────────────────────────────────────────────────────────
    private val idlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF666666.toInt()
        style = Paint.Style.FILL
        alpha = 80
    }

    private val pressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFAAAAAA.toInt()
        style = Paint.Style.FILL
        alpha = 160
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFCCCCCC.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
        alpha = 120
    }

    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
        alpha = 200
    }

    // ── reusable path for arrows ────────────────────────────────────────────
    private val arrowPath = Path()

    // ── InputManager ────────────────────────────────────────────────────────

    override fun setScreenSize(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height

        buttonSize = min(width, height) * 0.065f
        val gap = buttonSize * 0.2f

        // --- Left cluster (left / right) at bottom-left ────────────────────
        val clusterLeftX = buttonSize * 0.6f
        val clusterLeftY = screenHeight - buttonSize * 2.5f

        // Left button
        leftRect = RectF(
            clusterLeftX,
            clusterLeftY,
            clusterLeftX + buttonSize,
            clusterLeftY + buttonSize
        )

        // Right button (to the right of left, with a gap + one button width)
        val rightBtnX = clusterLeftX + buttonSize + gap + buttonSize + gap
        rightRect = RectF(
            rightBtnX,
            clusterLeftY,
            rightBtnX + buttonSize,
            clusterLeftY + buttonSize
        )

        // --- Right cluster (up / down) at bottom-right ─────────────────────
        val clusterRightX = screenWidth - buttonSize * 2.5f
        val clusterRightBaseY = screenHeight - buttonSize * 2.5f

        // Up button (above the down button)
        upRect = RectF(
            clusterRightX,
            clusterRightBaseY - buttonSize - gap,
            clusterRightX + buttonSize,
            clusterRightBaseY - gap
        )

        // Down button
        downRect = RectF(
            clusterRightX,
            clusterRightBaseY,
            clusterRightX + buttonSize,
            clusterRightBaseY + buttonSize
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Reset all buttons, then re-evaluate every active pointer.
        leftPressed = false
        rightPressed = false
        upPressed = false
        downPressed = false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val px = event.getX(i)
                    val py = event.getY(i)
                    if (leftRect.contains(px, py)) leftPressed = true
                    if (rightRect.contains(px, py)) rightPressed = true
                    if (upRect.contains(px, py)) upPressed = true
                    if (downRect.contains(px, py)) downPressed = true
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // All fingers lifted -- buttons already reset above.
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                // One finger lifted while others remain.  Re-check remaining.
                val liftedIndex = event.actionIndex
                for (i in 0 until event.pointerCount) {
                    if (i == liftedIndex) continue
                    val px = event.getX(i)
                    val py = event.getY(i)
                    if (leftRect.contains(px, py)) leftPressed = true
                    if (rightRect.contains(px, py)) rightPressed = true
                    if (upRect.contains(px, py)) upPressed = true
                    if (downRect.contains(px, py)) downPressed = true
                }
                return true
            }
        }
        return false
    }

    override fun getDirection(): Vector2 {
        var dx = 0f
        var dy = 0f

        if (leftPressed) dx -= 1f
        if (rightPressed) dx += 1f
        if (upPressed) dy -= 1f      // screen-space: up is negative Y
        if (downPressed) dy += 1f

        val raw = Vector2(dx, dy)
        return if (raw.magnitude() > 0f) raw.normalized() else Vector2.ZERO
    }

    override fun render(canvas: Canvas) {
        drawButton(canvas, leftRect, leftPressed, ArrowDir.LEFT)
        drawButton(canvas, rightRect, rightPressed, ArrowDir.RIGHT)
        drawButton(canvas, upRect, upPressed, ArrowDir.UP)
        drawButton(canvas, downRect, downPressed, ArrowDir.DOWN)
    }

    // ── drawing helpers ─────────────────────────────────────────────────────

    private enum class ArrowDir { LEFT, RIGHT, UP, DOWN }

    private fun drawButton(canvas: Canvas, rect: RectF, pressed: Boolean, dir: ArrowDir) {
        val cornerRadius = buttonSize * 0.2f

        // Background
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, if (pressed) pressedPaint else idlePaint)
        // Border
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        // Arrow glyph
        drawArrow(canvas, rect, dir)
    }

    private fun drawArrow(canvas: Canvas, rect: RectF, dir: ArrowDir) {
        val cx = rect.centerX()
        val cy = rect.centerY()
        val half = buttonSize * 0.25f

        arrowPath.reset()

        when (dir) {
            ArrowDir.LEFT -> {
                arrowPath.moveTo(cx - half, cy)
                arrowPath.lineTo(cx + half * 0.6f, cy - half)
                arrowPath.lineTo(cx + half * 0.6f, cy + half)
                arrowPath.close()
            }
            ArrowDir.RIGHT -> {
                arrowPath.moveTo(cx + half, cy)
                arrowPath.lineTo(cx - half * 0.6f, cy - half)
                arrowPath.lineTo(cx - half * 0.6f, cy + half)
                arrowPath.close()
            }
            ArrowDir.UP -> {
                arrowPath.moveTo(cx, cy - half)
                arrowPath.lineTo(cx - half, cy + half * 0.6f)
                arrowPath.lineTo(cx + half, cy + half * 0.6f)
                arrowPath.close()
            }
            ArrowDir.DOWN -> {
                arrowPath.moveTo(cx, cy + half)
                arrowPath.lineTo(cx - half, cy - half * 0.6f)
                arrowPath.lineTo(cx + half, cy - half * 0.6f)
                arrowPath.close()
            }
        }

        canvas.drawPath(arrowPath, arrowPaint)
    }
}
