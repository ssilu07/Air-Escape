package com.royals.airescape.entity

import android.graphics.Canvas
import android.graphics.Paint
import com.royals.airescape.util.Vector2

/**
 * Temporary floating text popup in world coordinates.
 * Floats upward and fades out (e.g. "+1 🪙" when star is collected).
 */
class FloatingText(
    var position: Vector2,
    val text: String,
    val color: Int = 0xFFFFD700.toInt(),
    val size: Float = 26f,
    private val maxLifetime: Float = 0.85f
) {
    var lifetime: Float = maxLifetime
    var alive: Boolean = true
        private set

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    fun update(dt: Float) {
        // Float upward
        position = Vector2(position.x, position.y - 60f * dt)
        lifetime -= dt
        if (lifetime <= 0f) {
            alive = false
        }
    }

    fun render(canvas: Canvas) {
        if (!alive) return
        val alpha = (lifetime / maxLifetime).coerceIn(0f, 1f)
        paint.textSize = size
        paint.color = color
        paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        canvas.drawText(text, position.x, position.y, paint)
    }
}
