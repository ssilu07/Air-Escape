package com.royals.airescape.entity

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.royals.airescape.data.Constants
import com.royals.airescape.util.Vector2

/**
 * A bullet fired by the player when the BULLET_SHOOT power-up is active.
 * Travels in a straight line and destroys enemy missiles on contact.
 */
class PlayerBullet(
    startPosition: Vector2,
    /** Direction angle in radians. */
    val angle: Float
) : Entity(
    position = startPosition,
    radius = Constants.PLAYER_BULLET_RADIUS
) {
    /** When set, the bitmap is drawn instead of a simple circle. */
    var bulletBitmap: Bitmap? = null

    private val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Constants.BULLET_SHOOT_COLOR.toInt()
        style = Paint.Style.FILL
    }

    override fun update(dt: Float) {
        velocity = Vector2.fromAngle(angle) * Constants.PLAYER_BULLET_SPEED
        position = position + velocity * dt
    }

    override fun render(canvas: Canvas) {
        val bmp = bulletBitmap
        if (bmp != null) {
            canvas.save()
            canvas.translate(position.x, position.y)
            // Bitmap points up; add 90 to align with movement direction
            canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat() + 90f)
            val halfW = bmp.width / 2f
            val halfH = bmp.height / 2f
            canvas.drawBitmap(bmp, -halfW, -halfH, null)
            canvas.restore()
        } else {
            canvas.drawCircle(position.x, position.y, radius, fallbackPaint)
        }
    }
}
