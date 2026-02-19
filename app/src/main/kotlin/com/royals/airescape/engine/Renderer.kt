package com.royals.airescape.engine

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.royals.airescape.util.Vector2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Canvas rendering utilities.
 * All [Paint] objects are allocated once and reused to avoid GC pressure.
 */
object Renderer {

    // Reusable Paint instances
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isFakeBoldText = true
    }

    private val reusablePath = Path()

    // ---- Text ----

    fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Long,
        align: Paint.Align = Paint.Align.LEFT
    ) {
        textPaint.textSize = size
        textPaint.color = color.toInt()
        textPaint.textAlign = align
        canvas.drawText(text, x, y, textPaint)
    }

    // ---- Circle ----

    fun drawCircle(
        canvas: Canvas,
        center: Vector2,
        radius: Float,
        color: Long,
        style: Paint.Style = Paint.Style.FILL
    ) {
        val paint = if (style == Paint.Style.FILL) fillPaint else strokePaint
        paint.color = color.toInt()
        paint.style = style
        canvas.drawCircle(center.x, center.y, radius, paint)
    }

    // ---- Star shape polygon ----

    fun drawStar(
        canvas: Canvas,
        center: Vector2,
        radius: Float,
        points: Int,
        color: Long
    ) {
        if (points < 2) return
        fillPaint.color = color.toInt()
        fillPaint.style = Paint.Style.FILL

        reusablePath.reset()
        val innerRadius = radius * 0.45f
        val totalPoints = points * 2
        val angleStep = (Math.PI * 2.0 / totalPoints).toFloat()
        // Start from top (negative Y is up on screen)
        val startAngle = (-Math.PI / 2.0).toFloat()

        for (i in 0 until totalPoints) {
            val r = if (i % 2 == 0) radius else innerRadius
            val angle = startAngle + i * angleStep
            val px = center.x + cos(angle) * r
            val py = center.y + sin(angle) * r
            if (i == 0) {
                reusablePath.moveTo(px, py)
            } else {
                reusablePath.lineTo(px, py)
            }
        }
        reusablePath.close()
        canvas.drawPath(reusablePath, fillPaint)
    }

    // ---- Triangle (for missiles) ----

    /**
     * Draws a filled triangle pointing in the direction of [angle] (radians).
     * The triangle's tip points forward.
     */
    fun drawTriangle(
        canvas: Canvas,
        center: Vector2,
        angle: Float,
        width: Float,
        height: Float,
        color: Long
    ) {
        fillPaint.color = color.toInt()
        fillPaint.style = Paint.Style.FILL

        val cosA = cos(angle)
        val sinA = sin(angle)

        // Tip (front)
        val tipX = center.x + cosA * height / 2f
        val tipY = center.y + sinA * height / 2f

        // Perpendicular direction for the base
        val perpX = -sinA
        val perpY = cosA

        // Back-left
        val blX = center.x - cosA * height / 2f + perpX * width / 2f
        val blY = center.y - sinA * height / 2f + perpY * width / 2f

        // Back-right
        val brX = center.x - cosA * height / 2f - perpX * width / 2f
        val brY = center.y - sinA * height / 2f - perpY * width / 2f

        reusablePath.reset()
        reusablePath.moveTo(tipX, tipY)
        reusablePath.lineTo(blX, blY)
        reusablePath.lineTo(brX, brY)
        reusablePath.close()
        canvas.drawPath(reusablePath, fillPaint)
    }

    // ---- Plane shape (player) ----

    /**
     * Draws a plane-like shape rotated by [angle].
     * [planeType] selects between different silhouettes:
     *   0 - simple dart / chevron
     *   1 - wider jet shape
     *   2 - rounded dot (fallback)
     */
    fun drawPlane(
        canvas: Canvas,
        center: Vector2,
        angle: Float,
        size: Float,
        color: Long,
        planeType: Int = 0
    ) {
        fillPaint.color = color.toInt()
        fillPaint.style = Paint.Style.FILL

        val cosA = cos(angle)
        val sinA = sin(angle)
        val perpX = -sinA
        val perpY = cosA

        reusablePath.reset()

        when (planeType) {
            0 -> {
                // Dart / chevron
                val nose = Vector2(
                    center.x + cosA * size,
                    center.y + sinA * size
                )
                val wingL = Vector2(
                    center.x - cosA * size * 0.5f + perpX * size * 0.7f,
                    center.y - sinA * size * 0.5f + perpY * size * 0.7f
                )
                val wingR = Vector2(
                    center.x - cosA * size * 0.5f - perpX * size * 0.7f,
                    center.y - sinA * size * 0.5f - perpY * size * 0.7f
                )
                val tail = Vector2(
                    center.x - cosA * size * 0.35f,
                    center.y - sinA * size * 0.35f
                )
                reusablePath.moveTo(nose.x, nose.y)
                reusablePath.lineTo(wingL.x, wingL.y)
                reusablePath.lineTo(tail.x, tail.y)
                reusablePath.lineTo(wingR.x, wingR.y)
                reusablePath.close()
            }
            1 -> {
                // Wider jet
                val nose = Vector2(
                    center.x + cosA * size * 1.1f,
                    center.y + sinA * size * 1.1f
                )
                val midL = Vector2(
                    center.x + perpX * size * 0.35f,
                    center.y + perpY * size * 0.35f
                )
                val midR = Vector2(
                    center.x - perpX * size * 0.35f,
                    center.y - perpY * size * 0.35f
                )
                val wingL = Vector2(
                    center.x - cosA * size * 0.3f + perpX * size * 0.9f,
                    center.y - sinA * size * 0.3f + perpY * size * 0.9f
                )
                val wingR = Vector2(
                    center.x - cosA * size * 0.3f - perpX * size * 0.9f,
                    center.y - sinA * size * 0.3f - perpY * size * 0.9f
                )
                val tailL = Vector2(
                    center.x - cosA * size * 0.8f + perpX * size * 0.4f,
                    center.y - sinA * size * 0.8f + perpY * size * 0.4f
                )
                val tailR = Vector2(
                    center.x - cosA * size * 0.8f - perpX * size * 0.4f,
                    center.y - sinA * size * 0.8f - perpY * size * 0.4f
                )
                reusablePath.moveTo(nose.x, nose.y)
                reusablePath.lineTo(midL.x, midL.y)
                reusablePath.lineTo(wingL.x, wingL.y)
                reusablePath.lineTo(tailL.x, tailL.y)
                reusablePath.lineTo(tailR.x, tailR.y)
                reusablePath.lineTo(wingR.x, wingR.y)
                reusablePath.lineTo(midR.x, midR.y)
                reusablePath.close()
            }
            else -> {
                // Fallback: simple circle
                canvas.drawCircle(center.x, center.y, size, fillPaint)
                return
            }
        }
        canvas.drawPath(reusablePath, fillPaint)
    }

    // ---- Button ----

    fun drawButton(
        canvas: Canvas,
        rect: RectF,
        text: String,
        color: Long,
        textColor: Long
    ) {
        fillPaint.color = color.toInt()
        fillPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, 16f, 16f, fillPaint)

        val textSize = (rect.height() * 0.45f).coerceAtMost(64f)
        textPaint.textSize = textSize
        textPaint.color = textColor.toInt()
        textPaint.textAlign = Paint.Align.CENTER

        // Vertically centre the text inside the rect
        val textY = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(text, rect.centerX(), textY, textPaint)
    }
}
