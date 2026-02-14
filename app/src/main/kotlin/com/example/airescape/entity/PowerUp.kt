package com.example.airescape.entity

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.example.airescape.data.Constants
import com.example.airescape.engine.Renderer
import com.example.airescape.util.Vector2
import kotlin.math.sin

/**
 * Types of power-ups available in the game.
 */
enum class PowerUpType {
    SHIELD,
    SPEED_BOOST
}

/**
 * A power-up pickup that grants a temporary ability.
 * Despawns after [DESPAWN_TIME] seconds if not collected.
 */
class PowerUp(
    startPosition: Vector2,
    val type: PowerUpType
) : Entity(
    position = startPosition,
    radius = Constants.POWERUP_RADIUS
) {
    companion object {
        /** How long the pickup stays on screen before disappearing. */
        const val DESPAWN_TIME = 10f
    }

    /** Timer driving the pulsing scale animation. */
    private var pulseTimer: Float = 0f

    /** Remaining time before the power-up despawns. */
    var lifetime: Float = DESPAWN_TIME
        private set

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    // ── Update ─────────────────────────────────────────────────────

    override fun update(dt: Float) {
        pulseTimer += dt
        lifetime -= dt

        if (lifetime <= 0f) {
            alive = false
        }
    }

    // ── Render ─────────────────────────────────────────────────────

    override fun render(canvas: Canvas) {
        val pulse = 1f + 0.15f * sin(pulseTimer * 5f)
        val drawRadius = radius * pulse

        // Flicker when about to despawn (last 3 seconds)
        if (lifetime < 3f) {
            val flicker = sin(pulseTimer * 20f)
            if (flicker < 0f) return // skip draw to create blinking
        }

        val color: Int
        val iconText: String

        when (type) {
            PowerUpType.SHIELD -> {
                color = Constants.SHIELD_COLOR.toInt()
                iconText = "S"
            }
            PowerUpType.SPEED_BOOST -> {
                color = Constants.SPEED_BOOST_COLOR.toInt()
                iconText = "\u26A1" // lightning bolt unicode
            }
        }

        // Background circle
        bgPaint.color = color
        bgPaint.alpha = 180
        canvas.drawCircle(position.x, position.y, drawRadius, bgPaint)

        // Outline
        outlinePaint.color = color
        outlinePaint.alpha = 255
        canvas.drawCircle(position.x, position.y, drawRadius, outlinePaint)

        // Icon text
        textPaint.color = 0xFFFFFFFF.toInt()
        textPaint.textSize = drawRadius * 1.1f
        val textYOffset = textPaint.textSize / 3f
        canvas.drawText(iconText, position.x, position.y + textYOffset, textPaint)
    }
}
