package com.royals.airescape.entity

import android.graphics.Canvas
import android.graphics.Paint
import com.royals.airescape.data.Constants
import com.royals.airescape.util.Vector2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A particle explosion effect.
 *
 * Not a subclass of [Entity] -- this is a standalone visual effect
 * that is updated and rendered by the [EntityManager].
 */
class Explosion(
    center: Vector2,
    baseColor: Int
) {
    /** A single particle within the explosion. */
    data class Particle(
        var position: Vector2,
        var velocity: Vector2,
        var color: Int,
        var radius: Float,
        var alpha: Float,
        var lifetime: Float
    )

    val particles: MutableList<Particle> = mutableListOf()

    /** False once all particles have faded out. */
    var alive: Boolean = true
        private set

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        val count = Constants.EXPLOSION_PARTICLE_COUNT
        for (i in 0 until count) {
            // Random direction
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = 80f + Random.nextFloat() * 220f
            val vx = cos(angle) * speed
            val vy = sin(angle) * speed

            // Slight colour variation
            val r = ((baseColor shr 16) and 0xFF).coerceIn(0, 255)
            val g = ((baseColor shr 8) and 0xFF).coerceIn(0, 255)
            val b = (baseColor and 0xFF).coerceIn(0, 255)
            val variation = Random.nextInt(-30, 31)
            val pr = (r + variation).coerceIn(0, 255)
            val pg = (g + variation).coerceIn(0, 255)
            val pb = (b + variation).coerceIn(0, 255)
            val particleColor = (0xFF shl 24) or (pr shl 16) or (pg shl 8) or pb

            particles.add(
                Particle(
                    position = center,
                    velocity = Vector2(vx, vy),
                    color = particleColor,
                    radius = 2f + Random.nextFloat() * 5f,
                    alpha = 1f,
                    lifetime = Constants.EXPLOSION_LIFETIME * (0.5f + Random.nextFloat() * 0.5f)
                )
            )
        }
    }

    // ── Update ─────────────────────────────────────────────────────

    fun update(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.position = p.position + p.velocity * dt
            p.lifetime -= dt

            // Shrink and fade
            val t = (p.lifetime / Constants.EXPLOSION_LIFETIME).coerceIn(0f, 1f)
            p.alpha = t
            p.radius *= (1f - 0.5f * dt) // gradual shrink

            // Slow down over time
            p.velocity = p.velocity * (1f - 1.5f * dt)

            if (p.lifetime <= 0f || p.alpha <= 0f) {
                iter.remove()
            }
        }

        alive = particles.isNotEmpty()
    }

    // ── Render ─────────────────────────────────────────────────────

    fun render(canvas: Canvas) {
        for (p in particles) {
            paint.color = p.color
            paint.alpha = (p.alpha * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(p.position.x, p.position.y, p.radius.coerceAtLeast(0.5f), paint)
        }
    }
}
