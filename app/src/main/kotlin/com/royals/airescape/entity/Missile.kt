package com.royals.airescape.entity

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import com.royals.airescape.data.Constants
import com.royals.airescape.data.GameData
import com.royals.airescape.engine.Renderer
import com.royals.airescape.util.Vector2
import kotlin.math.PI
import kotlin.math.atan2

/**
 * Types of missiles in the game.
 */
enum class MissileType {
    /** Classic homing missile — tracks the player, speed ramps over time. */
    HOMING,
    /** Fast straight-line missile — no tracking, high speed. */
    STRAIGHT,
    /** Bounces off viewport edges up to N times. */
    BOUNCING,
    /** Splits into mini missiles on death. */
    CLUSTER,
    /** Periodically turns invisible. */
    STEALTH
}

/**
 * A missile entity. Behaviour varies by [missileType].
 *
 * The default [MissileType.HOMING] gradually steers toward [targetPosition],
 * ramping speed from [Constants.MISSILE_INITIAL_SPEED] toward
 * [Constants.MISSILE_MAX_SPEED] over [Constants.MISSILE_SPEED_RAMP_TIME] seconds.
 */
class Missile(
    startPosition: Vector2,
    initialAngle: Float,
    /** Live reference to the position the missile should home toward. */
    var targetPosition: Vector2 = Vector2(),
    /** The type of this missile — determines behaviour and colour. */
    val missileType: MissileType = MissileType.HOMING
) : Entity(
    position = startPosition,
    radius = Constants.MISSILE_RADIUS
) {
    /** Current heading in radians. */
    var angle: Float = initialAngle

    /** Current forward speed (px / s). */
    var speed: Float = when (missileType) {
        MissileType.STRAIGHT -> Constants.STRAIGHT_MISSILE_SPEED
        MissileType.BOUNCING -> Constants.BOUNCING_MISSILE_SPEED
        else -> Constants.MISSILE_INITIAL_SPEED
    }

    /** How long this missile has been alive (used for speed ramp). */
    var spawnTime: Float = 0f

    /** Turn rate in rad / s. */
    var turnRate: Float = when (missileType) {
        MissileType.STRAIGHT -> 0f   // no homing
        else -> Constants.MISSILE_TURN_RATE
    }

    /** When set, the bitmap is drawn instead of the programmatic triangle. */
    var bulletBitmap: Bitmap? = null

    /** True when this is a mini-missile spawned by a cluster explosion. */
    var isClusterMini: Boolean = false

    /** Remaining lifetime for cluster mini missiles. */
    var remainingLifetime: Float = Float.MAX_VALUE

    // ── Bouncing state ───────────────────────────────────────────────
    var bounceCount: Int = 0

    // Viewport bounds for bounce detection (set by EntityManager)
    var viewLeft: Float = 0f
    var viewRight: Float = 0f
    var viewTop: Float = 0f
    var viewBottom: Float = 0f

    // ── Stealth state ────────────────────────────────────────────────
    private var stealthTimer: Float = 0f
    /** Whether the stealth missile is currently visible. */
    var stealthVisible: Boolean = true
        private set

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

    /** The ARGB colour used for the fallback triangle (when no bitmap). */
    val missileColor: Long
        get() = Constants.MISSILE_COLOR

    // ── Colorblind mode marker ──────────────────────────────────────
    private val cbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    // ── Update ─────────────────────────────────────────────────────

    override fun update(dt: Float) {
        spawnTime += dt

        // Lifetime check for cluster minis
        if (isClusterMini) {
            remainingLifetime -= dt
            if (remainingLifetime <= 0f) {
                alive = false
                return
            }
        }

        when (missileType) {
            MissileType.HOMING -> updateHoming(dt)
            MissileType.STRAIGHT -> updateStraight(dt)
            MissileType.BOUNCING -> updateBouncing(dt)
            MissileType.CLUSTER -> updateHoming(dt)   // cluster homes like normal until death
            MissileType.STEALTH -> updateStealth(dt)
        }

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

    /** Standard homing behaviour (used by HOMING and CLUSTER). */
    private fun updateHoming(dt: Float) {
        val dx = targetPosition.x - position.x
        val dy = targetPosition.y - position.y
        val desiredAngle = atan2(dy, dx)

        var diff = desiredAngle - angle
        while (diff > PI.toFloat()) diff -= (2f * PI).toFloat()
        while (diff < -PI.toFloat()) diff += (2f * PI).toFloat()

        // Speed ramp
        val rampT = (spawnTime / Constants.MISSILE_SPEED_RAMP_TIME).coerceIn(0f, 1f)
        speed = Constants.MISSILE_INITIAL_SPEED +
                (Constants.MISSILE_MAX_SPEED - Constants.MISSILE_INITIAL_SPEED) * rampT

        val effectiveTurnRate = turnRate * (Constants.MISSILE_INITIAL_SPEED / speed)
        val maxTurn = effectiveTurnRate * dt
        angle += diff.coerceIn(-maxTurn, maxTurn)

        velocity = Vector2.fromAngle(angle) * speed
        position = position + velocity * dt
    }

    /** Straight-line — no steering, constant high speed. */
    private fun updateStraight(dt: Float) {
        velocity = Vector2.fromAngle(angle) * speed
        position = position + velocity * dt
    }

    /** Bouncing — reflects off viewport edges. */
    private fun updateBouncing(dt: Float) {
        // Mild homing so it's not completely random
        val dx = targetPosition.x - position.x
        val dy = targetPosition.y - position.y
        val desiredAngle = atan2(dy, dx)
        var diff = desiredAngle - angle
        while (diff > PI.toFloat()) diff -= (2f * PI).toFloat()
        while (diff < -PI.toFloat()) diff += (2f * PI).toFloat()
        val mildTurn = turnRate * 0.3f * dt
        angle += diff.coerceIn(-mildTurn, mildTurn)

        velocity = Vector2.fromAngle(angle) * speed
        position = position + velocity * dt

        // Bounce off viewport edges
        var bounced = false
        if (position.x - radius <= viewLeft) {
            angle = PI.toFloat() - angle
            position = Vector2(viewLeft + radius, position.y)
            bounced = true
        } else if (position.x + radius >= viewRight) {
            angle = PI.toFloat() - angle
            position = Vector2(viewRight - radius, position.y)
            bounced = true
        }
        if (position.y - radius <= viewTop) {
            angle = -angle
            position = Vector2(position.x, viewTop + radius)
            bounced = true
        } else if (position.y + radius >= viewBottom) {
            angle = -angle
            position = Vector2(position.x, viewBottom - radius)
            bounced = true
        }
        if (bounced) {
            bounceCount++
            if (bounceCount >= Constants.BOUNCING_MISSILE_MAX_BOUNCES) {
                alive = false
            }
        }
    }

    /** Stealth — homing with periodic invisibility. */
    private fun updateStealth(dt: Float) {
        updateHoming(dt)

        stealthTimer += dt
        val cycleDuration = Constants.STEALTH_VISIBLE_TIME + Constants.STEALTH_INVISIBLE_TIME
        val phase = stealthTimer % cycleDuration
        stealthVisible = phase < Constants.STEALTH_VISIBLE_TIME
    }

    // ── Render ─────────────────────────────────────────────────────

    override fun render(canvas: Canvas) {
        // Stealth missiles: skip rendering when invisible
        if (missileType == MissileType.STEALTH && !stealthVisible) return

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
                missileColor
            )
        }

        // Colorblind mode: draw shape/letter markers to distinguish missile types
        if (GameData.colorblindMode && missileType != MissileType.HOMING) {
            cbPaint.textSize = radius * 1.2f
            val symbol = when (missileType) {
                MissileType.STRAIGHT -> "\u2192"  // arrow →
                MissileType.BOUNCING -> "\u25C7"  // diamond ◇
                MissileType.CLUSTER -> "\u2733"   // asterisk ✳
                MissileType.STEALTH -> "\u2026"   // ellipsis …
                else -> ""
            }
            canvas.drawText(symbol, position.x, position.y + cbPaint.textSize / 3f, cbPaint)
        }
    }
}
