package com.royals.airescape.level

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.royals.airescape.entity.Entity
import com.royals.airescape.util.Vector2
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Floating aerial magnetic mine.
 * Drifts passively until player enters proximity, then homes in slowly with a pulsing red warning.
 * Explodes violently on contact or when shot.
 */
class AirMine(
    startPos: Vector2
) : Entity(
    position = startPos,
    radius = 26f
) {
    private var timer: Float = 0f
    var triggered: Boolean = false
        private set

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF212121.toInt()
        style = Paint.Style.FILL
    }
    private val spikePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF424242.toInt()
        style = Paint.Style.FILL
    }
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF1744.toInt()
        style = Paint.Style.FILL
    }
    private val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF1744.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    override fun update(dt: Float) {
        timer += dt
    }

    fun updateWithPlayer(dt: Float, playerPos: Vector2) {
        timer += dt
        val dx = playerPos.x - position.x
        val dy = playerPos.y - position.y
        val dist = hypot(dx, dy)

        // Magnetic trigger radius (260 world units)
        if (dist < 260f) {
            triggered = true
            // Slowly drift towards player
            val angle = atan2(dy, dx)
            val driftSpeed = 65f
            position = Vector2(
                position.x + cos(angle) * driftSpeed * dt,
                position.y + sin(angle) * driftSpeed * dt
            )
        } else {
            triggered = false
            // Gentle floating bob
            position = Vector2(
                position.x + cos(timer * 2f) * 12f * dt,
                position.y + sin(timer * 2f) * 12f * dt
            )
        }
    }

    override fun render(canvas: Canvas) {
        if (!alive) return

        val pulseSpeed = if (triggered) 12f else 4f
        val pulse = 0.5f + 0.5f * sin(timer * pulseSpeed).toFloat()

        // 8 Spikes
        val spikeCount = 8
        val spikeLen = radius * 1.55f
        for (i in 0 until spikeCount) {
            val a = (i * Math.PI * 2.0 / spikeCount).toFloat() + timer * 0.3f
            val sx = position.x + cos(a) * spikeLen
            val sy = position.y + sin(a) * spikeLen
            canvas.drawLine(position.x, position.y, sx, sy, spikePaint)
            canvas.drawCircle(sx, sy, 3.5f, spikePaint)
        }

        // Main dark spherical shell
        canvas.drawCircle(position.x, position.y, radius, bodyPaint)

        // Inner glowing core
        corePaint.color = if (triggered) 0xFFFF1744.toInt() else 0xFFFF9100.toInt()
        val coreR = radius * 0.45f * (0.85f + 0.25f * pulse)
        canvas.drawCircle(position.x, position.y, coreR, corePaint)

        // Danger aura if triggered
        if (triggered) {
            auraPaint.alpha = (pulse * 180).toInt().coerceIn(0, 255)
            canvas.drawCircle(position.x, position.y, radius * 1.8f, auraPaint)
        }
    }
}
