package com.royals.airescape.level

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import com.royals.airescape.entity.Entity
import com.royals.airescape.util.Vector2
import kotlin.math.sin

/**
 * The Finish Line / Aircraft Carrier Landing Deck at the end of the level.
 * Crossing this landing zone triggers Mission Victory!
 */
class FinishLine(
    val yPosition: Float,
    val corridorWidth: Float = 2200f
) : Entity(
    position = Vector2(0f, yPosition),
    radius = corridorWidth / 2f
) {
    private var timer: Float = 0f

    private val deckRect = RectF()
    private val deckPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00E676.toInt()
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
        textSize = 42f
    }
    private val beaconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    override fun update(dt: Float) {
        timer += dt
    }

    /**
     * Checks if the player has crossed the finish threshold.
     */
    fun hasPlayerReached(playerPos: Vector2): Boolean {
        return playerPos.y <= (yPosition + 30f)
    }

    override fun render(canvas: Canvas) {
        val halfW = corridorWidth / 2f
        val deckH = 140f
        val top = yPosition - deckH / 2f
        val bottom = yPosition + deckH / 2f
        val left = -halfW
        val right = halfW

        deckRect.set(left, top, right, bottom)

        // Carrier Deck Background Gradient
        val deckShader = LinearGradient(
            0f, top, 0f, bottom,
            0xEE1A237E.toInt(), 0xEE0D47A1.toInt(), Shader.TileMode.CLAMP
        )
        deckPaint.shader = deckShader
        canvas.drawRoundRect(deckRect, 16f, 16f, deckPaint)
        deckPaint.shader = null

        // Glowing Arrival Boundary
        val pulse = 0.7f + 0.3f * sin(timer * 6f).toFloat()
        linePaint.alpha = (pulse * 255).toInt().coerceIn(0, 255)
        canvas.drawLine(left, bottom, right, bottom, linePaint)
        canvas.drawLine(left, top, right, top, linePaint)

        // Runway Beacons / Lights along the deck
        val beaconCount = 14
        val step = corridorWidth / (beaconCount - 1)
        for (i in 0 until beaconCount) {
            val bx = left + i * step
            beaconPaint.color = if ((i + (timer * 5f).toInt()) % 2 == 0) 0xFF00E676.toInt() else 0xFF00B0FF.toInt()
            canvas.drawCircle(bx, top + 12f, 7f, beaconPaint)
            canvas.drawCircle(bx, bottom - 12f, 7f, beaconPaint)
        }

        // Center Runway Chevrons & Text
        textPaint.textSize = 38f
        textPaint.color = 0xFFFFFFFF.toInt()
        canvas.drawText("▼ ▼  CARRIER EXTRACTION DECK  ▼ ▼", 0f, yPosition - 4f, textPaint)

        textPaint.textSize = 24f
        textPaint.color = 0xFF00E676.toInt()
        canvas.drawText("MISSION FINISH LINE", 0f, yPosition + 34f, textPaint)
    }
}
