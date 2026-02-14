package com.example.airescape.input

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import com.example.airescape.util.Vector2
import kotlin.math.min

/**
 * Virtual joystick — large, centered at the bottom of the screen.
 * Outer ring + inner ring + four directional arrows (like the reference game).
 */
class JoystickController : InputManager {

    // ── geometry ────────────────────────────────────────────────────────────
    private var baseCenter = Vector2.ZERO
    private var thumbPosition = Vector2.ZERO
    private var outerRadius = 120f
    private var innerRadius = 60f
    private var thumbRadius = 30f

    // ── state ───────────────────────────────────────────────────────────────
    private var active = false
    private var activePointerId = -1

    // ── screen dimensions ───────────────────────────────────────────────────
    private var screenWidth = 0f
    private var screenHeight = 0f

    // ── dead-zone threshold ─────────────────────────────────────────────────
    private val deadZone = 10f

    // ── paints ──────────────────────────────────────────────────────────────
    private val outerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
        alpha = 60
    }

    private val outerRingActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
        alpha = 100
    }

    private val innerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        alpha = 70
    }

    private val innerRingActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        alpha = 130
    }

    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
        alpha = 100
    }

    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
        alpha = 80
    }

    private val arrowActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
        alpha = 160
    }

    private val arrowPath = Path()

    // ── InputManager ────────────────────────────────────────────────────────

    override fun setScreenSize(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
        // Large joystick centered at bottom
        outerRadius = min(width, height) * 0.16f
        innerRadius = outerRadius * 0.5f
        thumbRadius = innerRadius * 0.45f
        baseCenter = Vector2(width / 2f, height - outerRadius * 1.4f)
        thumbPosition = baseCenter
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                val touchX = event.getX(pointerIndex)
                val touchY = event.getY(pointerIndex)
                val touch = Vector2(touchX, touchY)

                if (!active && touch.distance(baseCenter) <= outerRadius * 1.5f) {
                    active = true
                    activePointerId = event.getPointerId(pointerIndex)
                    thumbPosition = clampThumb(touch)
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (active) {
                    val pointerIndex = event.findPointerIndex(activePointerId)
                    if (pointerIndex != -1) {
                        val touch = Vector2(event.getX(pointerIndex), event.getY(pointerIndex))
                        thumbPosition = clampThumb(touch)
                        return true
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (active) {
                    active = false
                    activePointerId = -1
                    thumbPosition = baseCenter
                    return true
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    active = false
                    activePointerId = -1
                    thumbPosition = baseCenter
                    return true
                }
            }
        }
        return false
    }

    override fun getDirection(): Vector2 {
        val delta = thumbPosition - baseCenter
        return if (delta.magnitude() > deadZone) delta.normalized() else Vector2.ZERO
    }

    override fun render(canvas: Canvas) {
        val outerRing = if (active) outerRingActivePaint else outerRingPaint
        val innerRing = if (active) innerRingActivePaint else innerRingPaint
        val arrow = if (active) arrowActivePaint else arrowPaint

        // Outer ring
        canvas.drawCircle(baseCenter.x, baseCenter.y, outerRadius, outerRing)

        // Inner ring
        canvas.drawCircle(baseCenter.x, baseCenter.y, innerRadius, innerRing)

        // Direction arrows inside inner ring
        val arrowDist = innerRadius * 0.65f
        val arrowSize = innerRadius * 0.2f
        drawArrow(canvas, baseCenter.x, baseCenter.y - arrowDist, arrowSize, 0f, arrow)   // up
        drawArrow(canvas, baseCenter.x, baseCenter.y + arrowDist, arrowSize, 180f, arrow)  // down
        drawArrow(canvas, baseCenter.x - arrowDist, baseCenter.y, arrowSize, -90f, arrow)  // left
        drawArrow(canvas, baseCenter.x + arrowDist, baseCenter.y, arrowSize, 90f, arrow)   // right

        // Thumb (only when active)
        if (active) {
            canvas.drawCircle(thumbPosition.x, thumbPosition.y, thumbRadius, thumbPaint)
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun drawArrow(canvas: Canvas, cx: Float, cy: Float, size: Float, rotation: Float, paint: Paint) {
        arrowPath.reset()
        // Triangle pointing up (before rotation)
        arrowPath.moveTo(cx, cy - size)
        arrowPath.lineTo(cx - size * 0.7f, cy + size * 0.3f)
        arrowPath.lineTo(cx + size * 0.7f, cy + size * 0.3f)
        arrowPath.close()

        if (rotation != 0f) {
            val matrix = android.graphics.Matrix()
            matrix.setRotate(rotation, cx, cy)
            arrowPath.transform(matrix)
        }
        canvas.drawPath(arrowPath, paint)
    }

    private fun clampThumb(touch: Vector2): Vector2 {
        val offset = touch - baseCenter
        return if (offset.magnitude() > outerRadius) {
            baseCenter + offset.normalized() * outerRadius
        } else {
            touch
        }
    }
}
