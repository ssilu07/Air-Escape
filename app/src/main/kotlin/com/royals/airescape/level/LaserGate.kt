package com.royals.airescape.level

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.royals.airescape.entity.Entity
import com.royals.airescape.util.Vector2
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Pulsing high-energy laser barrier spanning horizontally across the flight corridor.
 * Cycles between ACTIVE (deadly beam) and INACTIVE (safe window to slip through).
 */
class LaserGate(
    val yPosition: Float,
    val xStart: Float,
    val xEnd: Float,
    private val cyclePeriod: Float = 3.6f, // total time of one on/off cycle
    private val activeRatio: Float = 0.65f // active 65% of the time
) : Entity(
    position = Vector2((xStart + xEnd) / 2f, yPosition),
    radius = (xEnd - xStart) / 2f
) {
    private var timer: Float = 0f
    var isBeamActive: Boolean = true
        private set

    private val pylonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF37474F.toInt()
        style = Paint.Style.FILL
    }
    private val pylonLedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val beamCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val beamGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 16f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    override fun update(dt: Float) {
        timer += dt
        val cycleTime = timer % cyclePeriod
        isBeamActive = cycleTime < (cyclePeriod * activeRatio)
    }

    /**
     * Checks if a point entity (like player or missile) collides with the active laser beam.
     */
    fun checkCollision(targetPos: Vector2, targetRadius: Float): Boolean {
        if (!isBeamActive || !alive) return false

        // Check vertical proximity to the laser line
        if (targetPos.y in (yPosition - targetRadius - 4f)..(yPosition + targetRadius + 4f)) {
            // Check horizontal bounds
            if (targetPos.x in (xStart - targetRadius)..(xEnd + targetRadius)) {
                return true
            }
        }
        return false
    }

    override fun render(canvas: Canvas) {
        if (!alive) return

        val pylonRadius = 18f
        val pulse = 0.6f + 0.4f * sin(timer * 10f).toFloat()

        // 1. Draw Pylons at both ends
        canvas.drawCircle(xStart, yPosition, pylonRadius, pylonPaint)
        canvas.drawCircle(xEnd, yPosition, pylonRadius, pylonPaint)

        pylonLedPaint.color = if (isBeamActive) 0xFFFF1744.toInt() else 0xFF00E676.toInt()
        canvas.drawCircle(xStart, yPosition, pylonRadius * 0.5f, pylonLedPaint)
        canvas.drawCircle(xEnd, yPosition, pylonRadius * 0.5f, pylonLedPaint)

        // 2. Draw Beam
        if (isBeamActive) {
            // Outer radiant glow
            beamGlowPaint.color = 0xFFFF1744.toInt()
            beamGlowPaint.alpha = (140 * pulse).toInt().coerceIn(0, 255)
            canvas.drawLine(xStart, yPosition, xEnd, yPosition, beamGlowPaint)

            // Inner hot core
            beamCorePaint.color = 0xFFFFFFFF.toInt()
            canvas.drawLine(xStart, yPosition, xEnd, yPosition, beamCorePaint)
        } else {
            // Charging warning line (safe to cross, but warning that it will turn on soon)
            val cycleTime = timer % cyclePeriod
            val timeLeft = (cyclePeriod - cycleTime)
            if (timeLeft < 0.8f) { // Warning right before activating
                beamCorePaint.color = 0xFFFF9100.toInt()
                beamCorePaint.alpha = ((1f - timeLeft / 0.8f) * 200).toInt().coerceIn(0, 255)
                canvas.drawLine(xStart, yPosition, xEnd, yPosition, beamCorePaint)
            }
        }
    }
}
