package com.royals.airescape.entity

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import com.royals.airescape.data.Constants
import com.royals.airescape.engine.Renderer
import com.royals.airescape.util.Vector2
import kotlin.math.PI
import kotlin.math.atan2

/**
 * A homing missile that tracks the player.
 *
 * The missile gradually steers toward [targetPosition] each frame.
 * Its speed ramps from [Constants.MISSILE_INITIAL_SPEED] toward
 * [Constants.MISSILE_MAX_SPEED] over [Constants.MISSILE_SPEED_RAMP_TIME] seconds.
 */
class Missile(
    startPosition: Vector2,
    initialAngle: Float,
    /** Live reference to the position the missile should home toward. */
    var targetPosition: Vector2 = Vector2()
) : Entity(
    position = startPosition,
    radius = Constants.MISSILE_RADIUS
) {
    /** Current heading in radians. */
    var angle: Float = initialAngle

    /** Current forward speed (px / s). */
    var speed: Float = Constants.MISSILE_INITIAL_SPEED

    /** How long this missile has been alive (used for speed ramp). */
    var spawnTime: Float = 0f

    /** Turn rate in rad / s. */
    var turnRate: Float = Constants.MISSILE_TURN_RATE

    /** When set, the bitmap is drawn instead of the programmatic triangle. */
    var bulletBitmap: Bitmap? = null

    // ── Dashed smoke trail ──────────────────────────────────────────
    private val trailPositions = mutableListOf<Vector2>()
    private var trailSpawnAccumulator: Float = 0f
    private val maxTrailPoints = 60

    private val trailPath = Path()
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        alpha = 200
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
    }

    // ── Update ─────────────────────────────────────────────────────

    override fun update(dt: Float) {
        spawnTime += dt

        // Desired angle toward target
        val dx = targetPosition.x - position.x
        val dy = targetPosition.y - position.y
        val desiredAngle = atan2(dy, dx)

        // Normalise angle difference to -PI..PI
        var diff = desiredAngle - angle
        while (diff > PI.toFloat()) diff -= (2f * PI).toFloat()
        while (diff < -PI.toFloat()) diff += (2f * PI).toFloat()

        // Speed ramp
        val rampT = (spawnTime / Constants.MISSILE_SPEED_RAMP_TIME).coerceIn(0f, 1f)
        speed = Constants.MISSILE_INITIAL_SPEED +
                (Constants.MISSILE_MAX_SPEED - Constants.MISSILE_INITIAL_SPEED) * rampT

        // Steer toward target — turn rate scales DOWN with speed
        // Fast missiles overshoot and make wide arcs before coming back
        val effectiveTurnRate = turnRate * (Constants.MISSILE_INITIAL_SPEED / speed)
        val maxTurn = effectiveTurnRate * dt
        angle += diff.coerceIn(-maxTurn, maxTurn)

        // Velocity & position
        velocity = Vector2.fromAngle(angle) * speed
        position = position + velocity * dt

        // Dashed trail – store positions at regular intervals
        trailSpawnAccumulator += dt
        val spawnInterval = 0.025f
        while (trailSpawnAccumulator >= spawnInterval) {
            trailSpawnAccumulator -= spawnInterval
            trailPositions.add(position)
            if (trailPositions.size > maxTrailPoints) {
                trailPositions.removeAt(0)
            }
        }

    }

    // ── Render ─────────────────────────────────────────────────────

    override fun render(canvas: Canvas) {
        // Dashed smoke trail (behind the missile)
        if (trailPositions.size >= 2) {
            trailPath.reset()
            trailPath.moveTo(trailPositions[0].x, trailPositions[0].y)
            for (i in 1 until trailPositions.size) {
                trailPath.lineTo(trailPositions[i].x, trailPositions[i].y)
            }
            canvas.drawPath(trailPath, trailPaint)
        }

        // Missile body (bitmap or fallback triangle)
        val bmp = bulletBitmap
        if (bmp != null) {
            canvas.save()
            canvas.translate(position.x, position.y)
            // Bitmap points up (-90°); add 90° to align with movement direction
            canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat() + 90f)
            val halfW = bmp.width / 2f
            val halfH = bmp.height / 2f
            canvas.drawBitmap(bmp, -halfW, -halfH, null)
            canvas.restore()
        } else {
            Renderer.drawTriangle(
                canvas,
                position,
                angle,
                radius * 0.8f,
                radius * 2f,
                Constants.MISSILE_COLOR
            )
        }
    }
}
