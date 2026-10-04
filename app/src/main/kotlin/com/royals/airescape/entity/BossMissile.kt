package com.royals.airescape.entity

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.royals.airescape.data.Constants
import com.royals.airescape.util.Vector2
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class BossMissile(
    startPosition: Vector2,
    var angle: Float,
    var targetPosition: Vector2,
    val maxHp: Int = Constants.BOSS_HP
) : Entity(
    position = startPosition,
    radius = Constants.BOSS_RADIUS
) {
    var hp: Int = maxHp
        private set
    private val speed: Float = Constants.BOSS_SPEED
    private val turnRate: Float = Constants.BOSS_TURN_RATE
    private var pulseTimer: Float = 0f

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Constants.BOSS_COLOR.toInt()
    }

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val hpBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF333333.toInt()
    }

    private val hpFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val hpBarRect = RectF()

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    override fun update(dt: Float) {
        pulseTimer += dt

        // Steer toward target
        val dx = targetPosition.x - position.x
        val dy = targetPosition.y - position.y
        val targetAngle = atan2(dy, dx)

        var angleDiff = targetAngle - angle
        while (angleDiff > Math.PI) angleDiff -= (Math.PI * 2).toFloat()
        while (angleDiff < -Math.PI) angleDiff += (Math.PI * 2).toFloat()

        val maxTurn = turnRate * dt
        angle += angleDiff.coerceIn(-maxTurn, maxTurn)

        // Move forward
        val vx = cos(angle) * speed
        val vy = sin(angle) * speed
        position = Vector2(position.x + vx * dt, position.y + vy * dt)
    }

    fun takeHit(): Boolean {
        hp--
        if (hp <= 0) {
            alive = false
            return true
        }
        return false
    }

    override fun render(canvas: Canvas) {
        val pulse = 1f + 0.05f * sin(pulseTimer * 5.0).toFloat()
        val curRadius = radius * pulse

        // Draw body
        bodyPaint.color = Constants.BOSS_COLOR.toInt()
        bodyPaint.alpha = 200
        canvas.drawCircle(position.x, position.y, curRadius, bodyPaint)

        // Draw glowing outline
        outlinePaint.color = 0xFFFF5252.toInt()
        val alpha = (180 + 75 * sin(pulseTimer * 5.0)).toInt().coerceIn(0, 255)
        outlinePaint.alpha = alpha
        canvas.drawCircle(position.x, position.y, curRadius, outlinePaint)

        // Draw BOSS text
        textPaint.textSize = curRadius * 0.9f
        val textY = position.y + textPaint.textSize / 3f
        canvas.drawText("BOSS", position.x, textY, textPaint)

        // Draw HP bar above boss
        val barWidth = radius * 3.0f
        val barHeight = 8f
        val barLeft = position.x - barWidth / 2f
        val barTop = position.y - curRadius - 18f

        hpBarRect.set(barLeft, barTop, barLeft + barWidth, barTop + barHeight)
        canvas.drawRoundRect(hpBarRect, 3f, 3f, hpBgPaint)

        val hpFraction = hp.toFloat() / maxHp.toFloat()
        hpFillPaint.color = when {
            hpFraction > 0.6f -> 0xFF00E676.toInt()
            hpFraction > 0.3f -> 0xFFFFEA00.toInt()
            else -> 0xFFFF1744.toInt()
        }
        hpBarRect.set(barLeft, barTop, barLeft + barWidth * hpFraction, barTop + barHeight)
        canvas.drawRoundRect(hpBarRect, 3f, 3f, hpFillPaint)
    }
}
