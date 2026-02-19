package com.royals.airescape.entity

import android.graphics.Bitmap
import android.graphics.Canvas
import com.royals.airescape.data.Constants
import com.royals.airescape.engine.SoundManager
import com.royals.airescape.util.CollisionUtil
import com.royals.airescape.util.Vector2
import kotlin.math.atan2
import kotlin.random.Random

/**
 * Central game orchestrator.
 *
 * Owns every game entity, handles spawning, collision detection,
 * scoring, camera tracking, and delegates update / render to each entity.
 */
class EntityManager {

    // ── Core entities ──────────────────────────────────────────────
    lateinit var player: Player
        private set
    val missiles: MutableList<Missile> = mutableListOf()
    val stars: MutableList<Star> = mutableListOf()
    val powerUps: MutableList<PowerUp> = mutableListOf()
    val explosions: MutableList<Explosion> = mutableListOf()
    val lottieBlasts: MutableList<LottieBlast> = mutableListOf()

    /** Bullet bitmap to apply to spawned missiles. */
    var bulletBitmap: Bitmap? = null

    // ── Camera (top-left corner of the viewport in world space) ───
    var cameraX: Float = 0f
        private set
    var cameraY: Float = 0f
        private set

    // ── Spawn timers ───────────────────────────────────────────────
    private var missileSpawnTimer: Float = 0f
    private var starSpawnTimer: Float = 0f
    private var powerUpSpawnTimer: Float = 0f
    private var currentMissileInterval: Float = Constants.MISSILE_SPAWN_INITIAL

    // ── Score / stats ──────────────────────────────────────────────
    var score: Int = 0
        private set
    var starsCollected: Int = 0
        private set
    var survivalTime: Float = 0f
        private set
    var gameOver: Boolean = false
        private set

    // ── Viewport dimensions (screen size) ──────────────────────────
    var screenWidth: Float = 1080f
        private set
    var screenHeight: Float = 1920f
        private set

    // ── Mode ───────────────────────────────────────────────────────
    var hardMode: Boolean = false
        private set

    // Accumulator for survival-time based scoring
    private var scoreTimeAccumulator: Float = 0f

    // ── Initialisation ─────────────────────────────────────────────

    fun init(
        screenWidth: Float,
        screenHeight: Float,
        planeType: Int = 0,
        planeColor: Int = Constants.PLAYER_DEFAULT_COLOR.toInt(),
        hardMode: Boolean = false
    ) {
        this.screenWidth = screenWidth
        this.screenHeight = screenHeight
        this.hardMode = hardMode

        // Clear collections
        missiles.clear()
        stars.clear()
        powerUps.clear()
        explosions.clear()
        lottieBlasts.clear()

        // Reset score / timers
        score = 0
        starsCollected = 0
        survivalTime = 0f
        gameOver = false
        missileSpawnTimer = 0f
        starSpawnTimer = 0f
        powerUpSpawnTimer = 0f
        currentMissileInterval = Constants.MISSILE_SPAWN_INITIAL
        scoreTimeAccumulator = 0f

        // Create player at origin (camera will center on it)
        player = Player().apply {
            position = Vector2(0f, 0f)
            this.planeType = planeType
            this.planeColor = planeColor
            this.hardMode = hardMode
        }

        // Initialize camera
        cameraX = player.position.x - screenWidth / 2f
        cameraY = player.position.y - screenHeight / 2f
    }

    // ── Update ─────────────────────────────────────────────────────

    fun update(dt: Float) {
        if (gameOver) {
            // Keep visual effects alive after death
            for (explosion in explosions) explosion.update(dt)
            for (blast in lottieBlasts) blast.update(dt)
            explosions.removeAll { !it.alive }
            lottieBlasts.removeAll { !it.alive }
            return
        }

        survivalTime += dt

        // ---- Spawning ------------------------------------------------

        // Missiles
        missileSpawnTimer += dt
        val effectiveInterval = if (hardMode) {
            currentMissileInterval / Constants.HARD_MODE_SPAWN_MULTIPLIER
        } else {
            currentMissileInterval
        }
        if (missileSpawnTimer >= effectiveInterval) {
            missileSpawnTimer = 0f
            spawnMissile()
            currentMissileInterval =
                (currentMissileInterval - Constants.MISSILE_SPAWN_DECREASE_RATE)
                    .coerceAtLeast(Constants.MISSILE_SPAWN_MIN)
        }

        // Stars
        starSpawnTimer += dt
        if (starSpawnTimer >= Constants.STAR_SPAWN_INTERVAL) {
            starSpawnTimer = 0f
            spawnStar()
        }

        // Power-ups
        powerUpSpawnTimer += dt
        if (powerUpSpawnTimer >= Constants.POWERUP_SPAWN_INTERVAL) {
            powerUpSpawnTimer = 0f
            spawnPowerUp()
        }

        // ---- Entity updates ------------------------------------------

        player.update(dt)

        // Camera follows the player (player stays centered)
        cameraX = player.position.x - screenWidth / 2f
        cameraY = player.position.y - screenHeight / 2f

        for (missile in missiles) {
            missile.targetPosition = player.position
            if (player.missileJammerActive) {
                // Jammed: missiles completely frozen, skip update
            } else if (player.slowMotionActive) {
                // Slow motion: missiles update at reduced speed
                missile.update(dt * Constants.SLOW_MOTION_MULTIPLIER)
            } else {
                missile.update(dt)
            }
        }
        for (star in stars) star.update(dt)
        for (powerUp in powerUps) powerUp.update(dt)
        for (explosion in explosions) explosion.update(dt)
        for (blast in lottieBlasts) blast.update(dt)

        // ---- Collision detection -------------------------------------

        checkMissilePlayerCollisions()
        checkMissileMissileCollisions()
        checkPlayerStarCollisions()
        checkPlayerPowerUpCollisions()

        // ---- Cleanup dead / off-camera entities ----------------------

        val missileMargin = 600f
        missiles.removeAll { m ->
            !m.alive || (
                m.position.x < cameraX - missileMargin ||
                m.position.x > cameraX + screenWidth + missileMargin ||
                m.position.y < cameraY - missileMargin ||
                m.position.y > cameraY + screenHeight + missileMargin
            )
        }

        val entityMargin = screenWidth
        stars.removeAll { s ->
            !s.alive || (
                s.position.x < cameraX - entityMargin ||
                s.position.x > cameraX + screenWidth + entityMargin ||
                s.position.y < cameraY - entityMargin ||
                s.position.y > cameraY + screenHeight + entityMargin
            )
        }

        powerUps.removeAll { p ->
            !p.alive || (
                p.position.x < cameraX - entityMargin ||
                p.position.x > cameraX + screenWidth + entityMargin ||
                p.position.y < cameraY - entityMargin ||
                p.position.y > cameraY + screenHeight + entityMargin
            )
        }

        explosions.removeAll { !it.alive }
        lottieBlasts.removeAll { !it.alive }

        // ---- Survival score ------------------------------------------

        scoreTimeAccumulator += dt
        if (scoreTimeAccumulator >= 1f) {
            score += if (player.doubleScoreActive) 2 else 1
            scoreTimeAccumulator -= 1f
        }
    }

    // ── Render ─────────────────────────────────────────────────────

    fun render(canvas: Canvas) {
        // Render order: stars -> power-ups -> missiles -> player -> explosions
        for (star in stars) star.render(canvas)
        for (powerUp in powerUps) powerUp.render(canvas)
        for (missile in missiles) missile.render(canvas)
        player.render(canvas)
        for (explosion in explosions) explosion.render(canvas)
        for (blast in lottieBlasts) LottieBlast.renderBlast(canvas, blast)
    }

    // ── Collision helpers ──────────────────────────────────────────

    private fun checkMissilePlayerCollisions() {
        val iter = missiles.iterator()
        while (iter.hasNext()) {
            val missile = iter.next()
            if (!missile.alive) continue

            if (CollisionUtil.circleCircle(
                    player.position, player.radius,
                    missile.position, missile.radius
                )
            ) {
                if (player.shieldActive) {
                    missile.alive = false
                    explosions.add(Explosion(missile.position, Constants.SHIELD_COLOR.toInt()))
                    lottieBlasts.add(LottieBlast(missile.position, size = 300f))
                    player.deactivateShield()
                    SoundManager.playShieldHit()
                } else {
                    gameOver = true
                    explosions.add(Explosion(player.position, Constants.MISSILE_COLOR.toInt()))
                    explosions.add(Explosion(player.position, Constants.PLAYER_DEFAULT_COLOR.toInt()))
                    lottieBlasts.add(LottieBlast(player.position, size = 500f))
                    SoundManager.playExplosion()
                    return
                }
            }
        }
    }

    private fun checkMissileMissileCollisions() {
        for (i in missiles.indices) {
            val a = missiles[i]
            if (!a.alive) continue
            for (j in i + 1 until missiles.size) {
                val b = missiles[j]
                if (!b.alive) continue

                if (CollisionUtil.circleCircle(
                        a.position, a.radius,
                        b.position, b.radius
                    )
                ) {
                    a.alive = false
                    b.alive = false
                    val mid = (a.position + b.position) * 0.5f
                    explosions.add(Explosion(mid, Constants.MISSILE_COLOR.toInt()))
                    lottieBlasts.add(LottieBlast(mid, size = 350f))
                    score += if (player.doubleScoreActive) 50 else 25
                    SoundManager.playMissileCollide()
                }
            }
        }
    }

    private fun checkPlayerStarCollisions() {
        val iter = stars.iterator()
        while (iter.hasNext()) {
            val star = iter.next()
            if (!star.alive) continue

            if (CollisionUtil.circleCircle(
                    player.position, player.radius,
                    star.position, star.radius
                )
            ) {
                star.alive = false
                starsCollected++
                score += if (player.doubleScoreActive) 20 else 10
                SoundManager.playStarCollect()
            }
        }
    }

    private fun checkPlayerPowerUpCollisions() {
        val iter = powerUps.iterator()
        while (iter.hasNext()) {
            val pu = iter.next()
            if (!pu.alive) continue

            if (CollisionUtil.circleCircle(
                    player.position, player.radius,
                    pu.position, pu.radius
                )
            ) {
                pu.alive = false
                when (pu.type) {
                    PowerUpType.SHIELD -> player.activateShield()
                    PowerUpType.SPEED_BOOST -> player.activateSpeedBoost()
                    PowerUpType.SLOW_MOTION -> player.activateSlowMotion()
                    PowerUpType.MISSILE_JAMMER -> player.activateMissileJammer()
                    PowerUpType.DOUBLE_SCORE -> player.activateDoubleScore()
                }
                SoundManager.playPowerUp()
            }
        }
    }

    // ── Spawners ───────────────────────────────────────────────────

    /**
     * Spawn a missile from a random edge of the camera view.
     */
    fun spawnMissile() {
        val viewLeft = cameraX
        val viewTop = cameraY
        val viewRight = cameraX + screenWidth
        val viewBottom = cameraY + screenHeight

        val edge = Random.nextInt(4)
        val margin = Constants.MISSILE_RADIUS * 2
        val spawnPos: Vector2 = when (edge) {
            0 -> Vector2(viewLeft + Random.nextFloat() * screenWidth, viewTop - margin)
            1 -> Vector2(viewLeft + Random.nextFloat() * screenWidth, viewBottom + margin)
            2 -> Vector2(viewLeft - margin, viewTop + Random.nextFloat() * screenHeight)
            else -> Vector2(viewRight + margin, viewTop + Random.nextFloat() * screenHeight)
        }

        val dx = player.position.x - spawnPos.x
        val dy = player.position.y - spawnPos.y
        val initialAngle = atan2(dy, dx)

        val missile = Missile(
            startPosition = spawnPos,
            initialAngle = initialAngle,
            targetPosition = player.position
        )
        missile.bulletBitmap = bulletBitmap
        if (hardMode) {
            missile.speed *= Constants.HARD_MODE_MISSILE_SPEED_MULT
            missile.turnRate *= Constants.HARD_MODE_TURN_RATE_MULT
        }
        missiles.add(missile)
    }

    /**
     * Spawn a star at a random position within the camera view.
     */
    fun spawnStar() {
        val margin = 40f
        val x = cameraX + margin + Random.nextFloat() * (screenWidth - margin * 2)
        val y = cameraY + margin + Random.nextFloat() * (screenHeight - margin * 2)
        stars.add(Star(Vector2(x, y)))
    }

    /**
     * Spawn a power-up at a random position within the camera view.
     */
    fun spawnPowerUp() {
        val margin = 50f
        val x = cameraX + margin + Random.nextFloat() * (screenWidth - margin * 2)
        val y = cameraY + margin + Random.nextFloat() * (screenHeight - margin * 2)
        val allTypes = PowerUpType.entries
        val type = allTypes[Random.nextInt(allTypes.size)]
        powerUps.add(PowerUp(Vector2(x, y), type))
    }
}
