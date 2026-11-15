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

        // Targeting laser guide line toward player (warning cue)
        val laserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFF1744.toInt()
            strokeWidth = 2f
            alpha = (90 + 70 * sin(pulseTimer * 8.0)).toInt().coerceIn(0, 255)
        }
        canvas.drawLine(position.x, position.y, targetPosition.x, targetPosition.y, laserPaint)

        // Draw mechanical armored vanes rotating around the core
        val vaneCount = 4
        val vaneStep = (Math.PI * 2.0 / vaneCount).toFloat()
        val vaneRot = pulseTimer * 1.5f
        val vanePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFF3D00.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        for (i in 0 until vaneCount) {
            val a = vaneRot + i * vaneStep
            val vx1 = position.x + cos(a) * curRadius * 0.9f
            val vy1 = position.y + sin(a) * curRadius * 0.9f
            val vx2 = position.x + cos(a) * curRadius * 1.35f
            val vy2 = position.y + sin(a) * curRadius * 1.35f
            canvas.drawLine(vx1, vy1, vx2, vy2, vanePaint)
        }

        // Draw body
        bodyPaint.color = 0xFF1A1A2E.toInt()
        bodyPaint.alpha = 240
        canvas.drawCircle(position.x, position.y, curRadius, bodyPaint)

        // Draw glowing inner energy core
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFF1744.toInt()
            style = Paint.Style.FILL
            alpha = (180 + 75 * sin(pulseTimer * 10.0)).toInt().coerceIn(0, 255)
        }
        canvas.drawCircle(position.x, position.y, curRadius * 0.55f, corePaint)

        // Draw glowing outline
        outlinePaint.color = 0xFFFF9100.toInt()
        val alpha = (200 + 55 * sin(pulseTimer * 5.0)).toInt().coerceIn(0, 255)
        outlinePaint.alpha = alpha
        outlinePaint.strokeWidth = 4.5f
        canvas.drawCircle(position.x, position.y, curRadius, outlinePaint)

        // Draw BOSS text
        textPaint.textSize = curRadius * 0.7f
        textPaint.color = 0xFFFFFFFF.toInt()
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
