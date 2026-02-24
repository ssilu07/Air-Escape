package com.royals.airescape.entity

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.royals.airescape.data.Constants
import com.royals.airescape.engine.Renderer
import com.royals.airescape.util.Vector2
import kotlin.math.PI
import kotlin.math.atan2

/**
 * The player-controlled plane entity.
 * The plane ALWAYS moves forward. [inputDirection] controls steering only —
 * the plane smoothly turns toward the joystick direction.
 */
class Player : Entity(
    radius = Constants.PLAYER_RADIUS
) {
    // ── Public input ───────────────────────────────────────────────
    /** Normalised direction from input. When non-zero the plane turns toward it. */
    var inputDirection: Vector2 = Vector2.ZERO

    // ── Visual / config ────────────────────────────────────────────
    var planeType: Int = 0
    var planeColor: Int = Constants.PLAYER_DEFAULT_COLOR.toInt()
    /** When set, the bitmap is drawn instead of the programmatic plane shape. */
    var planeBitmap: Bitmap? = null
    /** Rotation offset (degrees) so the bitmap nose aligns with movement direction. */
    var bitmapRotationOffset: Float = 90f

    /** Angle the plane is facing (radians, 0 = right). Starts pointing UP. */
    var angle: Float = (-PI / 2.0).toFloat()

    // ── Hard-mode flag (no player speed change, kept for future use) ──
    var hardMode: Boolean = false

    // ── Shield ─────────────────────────────────────────────────────
    var shieldActive: Boolean = false
        private set
    var shieldTimer: Float = 0f
        private set

    // ── Speed boost ────────────────────────────────────────────────
    var speedBoostActive: Boolean = false
        private set
    var speedBoostTimer: Float = 0f
        private set

    // ── Slow motion (slows missiles) ─────────────────────────────
    var slowMotionActive: Boolean = false
        private set
    var slowMotionTimer: Float = 0f
        private set

    // ── Missile jammer (freezes missiles) ────────────────────────
    var missileJammerActive: Boolean = false
        private set
    var missileJammerTimer: Float = 0f
        private set

    // ── Double score ─────────────────────────────────────────────
    var doubleScoreActive: Boolean = false
        private set
    var doubleScoreTimer: Float = 0f
        private set

    // ── Bullet shoot (player fires bullets at missiles) ────────
    var bulletShootActive: Boolean = false
        private set
    var bulletShootTimer: Float = 0f
        private set
    var bulletFireAccumulator: Float = 0f
    /** Plane has a permanent built-in gun (e.g. Jet, Rafale). */
    var hasBuiltInGun: Boolean = false

    // ── Trail particles ────────────────────────────────────────────
    data class TrailParticle(
        var position: Vector2,
        var alpha: Float,
        var lifetime: Float
    )

    private val trailParticles = mutableListOf<TrailParticle>()
    private var trailSpawnAccumulator: Float = 0f

    private val shieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // ── Public helpers ─────────────────────────────────────────────

    /** Effective movement speed accounting for boost. */
    fun getSpeed(): Float {
        var speed = Constants.PLAYER_SPEED
        if (speedBoostActive) speed *= Constants.SPEED_BOOST_MULTIPLIER
        return speed
    }

    fun activateShield() {
        shieldActive = true
    }

    fun activateSpeedBoost() {
        speedBoostActive = true
        speedBoostTimer = Constants.SPEED_BOOST_DURATION
    }

    fun deactivateShield() {
        shieldActive = false
        shieldTimer = 0f
    }

    fun activateSlowMotion() {
        slowMotionActive = true
        slowMotionTimer = Constants.SLOW_MOTION_DURATION
    }

    fun activateMissileJammer() {
        missileJammerActive = true
        missileJammerTimer = Constants.MISSILE_JAMMER_DURATION
    }

    fun activateDoubleScore() {
        doubleScoreActive = true
        doubleScoreTimer = Constants.DOUBLE_SCORE_DURATION
    }

    fun activateBulletShoot() {
        bulletShootActive = true
        bulletShootTimer = Constants.BULLET_SHOOT_DURATION
        bulletFireAccumulator = 0f
    }

    // ── Update ─────────────────────────────────────────────────────

    override fun update(dt: Float) {
        val speed = getSpeed()

        // Steer toward joystick direction when active
        if (inputDirection.magnitude() > 0.1f) {
            val targetAngle = atan2(inputDirection.y, inputDirection.x)
            var angleDiff = targetAngle - angle
            // Normalize to -PI..PI
            while (angleDiff > PI.toFloat()) angleDiff -= (2f * PI).toFloat()
            while (angleDiff < -PI.toFloat()) angleDiff += (2f * PI).toFloat()
            val maxTurn = Constants.PLAYER_TURN_RATE * dt
            angle += angleDiff.coerceIn(-maxTurn, maxTurn)
        }

        // ALWAYS move forward in the direction the plane faces
        velocity = Vector2.fromAngle(angle) * speed
        position = position + velocity * dt
        // No screen clamping — world is infinite, camera follows

        // Shield has no timer – it stays until a missile hit deactivates it

        // Speed boost timer
        if (speedBoostActive) {
            speedBoostTimer -= dt
            if (speedBoostTimer <= 0f) {
                speedBoostActive = false
                speedBoostTimer = 0f
            }
        }

        // Slow motion timer
        if (slowMotionActive) {
            slowMotionTimer -= dt
            if (slowMotionTimer <= 0f) {
                slowMotionActive = false
                slowMotionTimer = 0f
            }
        }

        // Missile jammer timer
        if (missileJammerActive) {
            missileJammerTimer -= dt
            if (missileJammerTimer <= 0f) {
                missileJammerActive = false
                missileJammerTimer = 0f
            }
        }

        // Double score timer
        if (doubleScoreActive) {
            doubleScoreTimer -= dt
            if (doubleScoreTimer <= 0f) {
                doubleScoreActive = false
                doubleScoreTimer = 0f
            }
        }

        // Bullet shoot timer
        if (bulletShootActive) {
            bulletShootTimer -= dt
            if (bulletShootTimer <= 0f) {
                bulletShootActive = false
                bulletShootTimer = 0f
            }
        }
        // Fire accumulator ticks when power-up active OR plane has built-in gun
        if (bulletShootActive || hasBuiltInGun) {
            bulletFireAccumulator += dt
        }

        // Trail particle spawning (always — plane is always moving)
        trailSpawnAccumulator += dt
        val spawnInterval = 0.02f
        while (trailSpawnAccumulator >= spawnInterval) {
            trailSpawnAccumulator -= spawnInterval
            trailParticles.add(
                TrailParticle(
                    position = position,
                    alpha = 1f,
                    lifetime = Constants.TRAIL_PARTICLE_LIFETIME
                )
            )
        }

        // Update existing trail particles
        val iter = trailParticles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.lifetime -= dt
            p.alpha = (p.lifetime / Constants.TRAIL_PARTICLE_LIFETIME).coerceIn(0f, 1f)
            if (p.lifetime <= 0f) iter.remove()
        }
    }

    // ── Render ─────────────────────────────────────────────────────

    override fun render(canvas: Canvas) {
        // Trail particles (drawn behind the plane)
        for (p in trailParticles) {
            trailPaint.color = planeColor
            trailPaint.alpha = (p.alpha * 150).toInt().coerceIn(0, 255)
            canvas.drawCircle(p.position.x, p.position.y, 3f, trailPaint)
        }

        // Plane (bitmap or fallback shape)
        val bmp = planeBitmap
        if (bmp != null) {
            canvas.save()
            canvas.translate(position.x, position.y)
            canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat() + bitmapRotationOffset)
            val halfW = bmp.width / 2f
            val halfH = bmp.height / 2f
            canvas.drawBitmap(bmp, -halfW, -halfH, null)
            canvas.restore()
        } else {
            Renderer.drawPlane(canvas, position, angle, radius, planeColor.toLong(), planeType)
        }

        // Shield overlay
        if (shieldActive) {
            val shieldRadius = radius + 10f
            shieldPaint.color = Constants.SHIELD_COLOR.toInt()
            shieldPaint.alpha = 120
            shieldPaint.style = Paint.Style.FILL
            canvas.drawCircle(position.x, position.y, shieldRadius, shieldPaint)

            shieldPaint.alpha = 200
            shieldPaint.style = Paint.Style.STROKE
            canvas.drawCircle(position.x, position.y, shieldRadius, shieldPaint)
        }
    }
}
