package com.example.airescape.entity

import android.graphics.Canvas
import android.graphics.Paint
import com.example.airescape.data.Constants
import com.example.airescape.engine.Renderer
import com.example.airescape.util.Vector2
import kotlin.math.sin

/**
 * A collectible star that bobs gently and sparkles.
 * Gives the player score when picked up.
 */
class Star(
    startPosition: Vector2
) : Entity(
    position = startPosition,
    radius = Constants.STAR_RADIUS
) {
    /** Timer driving the up-down bobbing sine wave. */
    private var floatTimer: Float = 0f

    /** Timer driving the sparkle / pulsing glow. */
    private var sparkleTimer: Float = 0f

    /** The base Y position (bobbing oscillates around this). */
    private val baseY: Float = startPosition.y

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // ── Update ─────────────────────────────────────────────────────

    override fun update(dt: Float) {
        floatTimer += dt
        sparkleTimer += dt

        // Gentle sine-wave bob
        val bobAmount = 6f // pixels of vertical movement
        val bobSpeed = 2.5f // radians per second
        position = Vector2(position.x, baseY + sin(floatTimer * bobSpeed) * bobAmount)
    }

    // ── Render ─────────────────────────────────────────────────────

    override fun render(canvas: Canvas) {
        // Pulsing glow behind the star
        val pulse = 0.8f + 0.2f * sin(sparkleTimer * 6f)
        val glowRadius = radius * 1.6f * pulse

        glowPaint.color = Constants.STAR_COLOR.toInt()
        glowPaint.alpha = (60 * pulse).toInt().coerceIn(0, 255)
        glowPaint.style = Paint.Style.FILL
        canvas.drawCircle(position.x, position.y, glowRadius, glowPaint)

        // Star shape
        Renderer.drawStar(
            canvas,
            position,
            radius * pulse,
            5,
            Constants.STAR_COLOR
        )
    }
}
