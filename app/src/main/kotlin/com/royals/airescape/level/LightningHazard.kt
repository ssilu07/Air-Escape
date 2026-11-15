package com.royals.airescape.level

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.royals.airescape.entity.Entity
import com.royals.airescape.util.Vector2
import kotlin.math.hypot
import kotlin.random.Random

/**
 * Atmospheric lightning hazard.
 * Gives a 0.8s warning tracer line before zapping a lethal high-voltage electric arc across the sky.
 */
class LightningHazard(
    val xStart: Float,
    val yStart: Float,
    val xEnd: Float,
    val yEnd: Float,
    val strikeInterval: Float = 5.0f
) : Entity(
    position = Vector2((xStart + xEnd) / 2f, (yStart + yEnd) / 2f),
    radius = 30f
) {
    private var timer: Float = Random.nextFloat() * 2f
    var isWarning: Boolean = false
        private set
    var isStriking: Boolean = false
        private set

    private val warningPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF1744.toInt()
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
    }

    private val lightningGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00E5FF.toInt()
        strokeWidth = 22f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val lightningCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val boltPath = Path()

    override fun update(dt: Float) {
        timer += dt
        val cycle = timer % strikeInterval
        isWarning = cycle in (strikeInterval - 1.0f)..(strikeInterval - 0.25f)
        isStriking = cycle > (strikeInterval - 0.25f)

        if (isStriking && boltPath.isEmpty) {
            generateJaggedBolt()
        } else if (!isStriking) {
            boltPath.reset()
        }
    }

    private fun generateJaggedBolt() {
        boltPath.reset()
        boltPath.moveTo(xStart, yStart)
        val segments = 8
        var curX = xStart
        var curY = yStart
        for (i in 1 until segments) {
            val frac = i.toFloat() / segments
            val targetX = xStart + (xEnd - xStart) * frac
            val targetY = yStart + (yEnd - yStart) * frac
            val jitter = (Random.nextFloat() - 0.5f) * 60f
            curX = targetX + jitter
            curY = targetY
            boltPath.lineTo(curX, curY)
        }
        boltPath.lineTo(xEnd, yEnd)
    }

    fun checkCollision(targetPos: Vector2, targetRadius: Float): Boolean {
        if (!isStriking || !alive) return false

        // Distance from point to line segment
        val lineLen = hypot(xEnd - xStart, yEnd - yStart)
        if (lineLen <= 0f) return false

        val u = (((targetPos.x - xStart) * (xEnd - xStart) + (targetPos.y - yStart) * (yEnd - yStart)) / (lineLen * lineLen)).coerceIn(0f, 1f)
        val nearestX = xStart + u * (xEnd - xStart)
        val nearestY = yStart + u * (yEnd - yStart)

        val dist = hypot(targetPos.x - nearestX, targetPos.y - nearestY)
        return dist < (targetRadius + 20f)
    }

    override fun render(canvas: Canvas) {
        if (!alive) return

        if (isWarning) {
            // Flashing warning tracer
            warningPaint.alpha = ((timer * 15f).toInt() % 2) * 220 + 35
            canvas.drawLine(xStart, yStart, xEnd, yEnd, warningPaint)
        } else if (isStriking) {
            // Thunder flash
            if (!boltPath.isEmpty) {
                canvas.drawPath(boltPath, lightningGlowPaint)
                canvas.drawPath(boltPath, lightningCorePaint)
            }
        }
    }
}
