package com.royals.airescape.entity

import android.graphics.Bitmap
import android.graphics.Canvas
import com.royals.airescape.data.Constants
import com.royals.airescape.data.PlaneAbility
import com.royals.airescape.engine.SoundManager
import com.royals.airescape.util.CollisionUtil
import com.royals.airescape.util.Vector2
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ln
import kotlin.math.sqrt
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
    val playerBullets: MutableList<PlayerBullet> = mutableListOf()
    val explosions: MutableList<Explosion> = mutableListOf()
    val lottieBlasts: MutableList<LottieBlast> = mutableListOf()
    val bosses: MutableList<BossMissile> = mutableListOf()

    /** Bullet bitmap to apply to spawned missiles (homing). */
    var bulletBitmap: Bitmap? = null

    /** Green bullet bitmap for player bullets. */
    var bulletGreenBitmap: Bitmap? = null

    /** Missile-type specific bitmaps. */
    var missileStraightBitmap: Bitmap? = null
    var missileBouncingBitmap: Bitmap? = null
    var missileClusterBitmap: Bitmap? = null
    var missileStealthBitmap: Bitmap? = null

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

    // ── Combo / streak system ────────────────────────────────────────
    /** Consecutive stars collected without being hit. */
    var starStreak: Int = 0
        private set
    /** Missiles destroyed without player taking a hit. */
    var missileDestroyStreak: Int = 0
        private set
    /** True when star streak bonus is active (visual feedback). */
    var comboActive: Boolean = false
        private set
    /** Total missiles destroyed this game (for daily challenges). */
    var missilesDestroyed: Int = 0
        private set
    /** Shields collected this game (for daily challenges). */
    var shieldsCollected: Int = 0
        private set

    // ── Boss fight ───────────────────────────────────────────────────
    private var bossSpawnTimer: Float = 0f
    private var nextBossTime: Float = Constants.BOSS_SPAWN_INTERVAL_MIN
    /** True while a boss is alive on screen. */
    val bossAlive: Boolean get() = bosses.any { it.alive }

    // ── Spawn counter for logarithmic difficulty ─────────────────────
    private var spawnCount: Int = 0

    // ── Screen shake ─────────────────────────────────────────────────
    var shakeTimer: Float = 0f
        private set
    var shakeIntensity: Float = 0f
        private set

    /** Current shake offset for camera (read by GameScreen). */
    var shakeOffsetX: Float = 0f
        private set
    var shakeOffsetY: Float = 0f
        private set

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
        playerBullets.clear()
        explosions.clear()
        lottieBlasts.clear()
        bosses.clear()

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
        starStreak = 0
        missileDestroyStreak = 0
        comboActive = false
        missilesDestroyed = 0
        shieldsCollected = 0
        bossSpawnTimer = 0f
        nextBossTime = Constants.BOSS_SPAWN_INTERVAL_MIN +
            Random.nextFloat() * (Constants.BOSS_SPAWN_INTERVAL_MAX - Constants.BOSS_SPAWN_INTERVAL_MIN)
        spawnCount = 0
        shakeTimer = 0f
        shakeIntensity = 0f
        shakeOffsetX = 0f
        shakeOffsetY = 0f

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

        // Missiles — logarithmic difficulty curve with missile cap
        missileSpawnTimer += dt
        // Slower ramp: ln(2) / ln(spawnCount + 3) — +3 instead of +2 for gentler curve
        currentMissileInterval = (Constants.MISSILE_SPAWN_INITIAL * ln(2.0) / ln(spawnCount.toDouble() + 3.0))
            .toFloat().coerceAtLeast(Constants.MISSILE_SPAWN_MIN)
        val effectiveInterval = if (hardMode) {
            currentMissileInterval / Constants.HARD_MODE_SPAWN_MULTIPLIER
        } else {
            currentMissileInterval
        }
        val maxMissiles = if (hardMode) Constants.MAX_MISSILES_HARD else Constants.MAX_MISSILES_NORMAL
        if (missileSpawnTimer >= effectiveInterval && missiles.size < maxMissiles) {
            missileSpawnTimer = 0f
            spawnMissile()
            spawnCount++
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

        // Boss spawn
        bossSpawnTimer += dt
        if (bossSpawnTimer >= nextBossTime && !bossAlive) {
            bossSpawnTimer = 0f
            spawnBoss()
            nextBossTime = Constants.BOSS_SPAWN_INTERVAL_MIN +
                Random.nextFloat() * (Constants.BOSS_SPAWN_INTERVAL_MAX - Constants.BOSS_SPAWN_INTERVAL_MIN)
        }

        // ---- Player bullet auto-fire -----------------------------------
        if ((player.bulletShootActive || player.hasBuiltInGun) && player.bulletFireAccumulator >= Constants.PLAYER_BULLET_FIRE_RATE) {
            player.bulletFireAccumulator -= Constants.PLAYER_BULLET_FIRE_RATE
            val bullet = PlayerBullet(player.position, player.angle)
            bullet.bulletBitmap = bulletGreenBitmap
            playerBullets.add(bullet)
            SoundManager.playBulletFire()
        }

        // ---- Entity updates ------------------------------------------

        player.update(dt)

        // Camera follows the player (player stays centered)
        cameraX = player.position.x - screenWidth / 2f
        cameraY = player.position.y - screenHeight / 2f

        for (missile in missiles) {
            missile.targetPosition = player.position
            // Supply viewport bounds for bouncing missiles
            if (missile.missileType == MissileType.BOUNCING) {
                missile.viewLeft = cameraX
                missile.viewTop = cameraY
                missile.viewRight = cameraX + screenWidth
                missile.viewBottom = cameraY + screenHeight
            }
            if (player.missileJammerActive) {
                // Jammed: missiles completely frozen, skip update
            } else if (player.slowMotionActive) {
                // Slow motion: missiles update at reduced speed
                missile.update(dt * Constants.SLOW_MOTION_MULTIPLIER)
            } else {
                missile.update(dt)
            }
        }
        for (playerBullet in playerBullets) playerBullet.update(dt)
        for (star in stars) star.update(dt)
        for (powerUp in powerUps) powerUp.update(dt)
        for (boss in bosses) {
            boss.targetPosition = player.position
            boss.update(dt)
        }
        for (explosion in explosions) explosion.update(dt)
        for (blast in lottieBlasts) blast.update(dt)

        // ---- Star magnet ability ------------------------------------
        if (player.ability == PlaneAbility.STAR_MAGNET) {
            val magnetRadius = Constants.STAR_MAGNET_RADIUS
            val magnetForce = Constants.STAR_MAGNET_FORCE
            for (star in stars) {
                if (!star.alive) continue
                val dx = player.position.x - star.position.x
                val dy = player.position.y - star.position.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < magnetRadius && dist > 1f) {
                    val pull = magnetForce * dt / dist
                    star.position = Vector2(
                        star.position.x + dx * pull,
                        star.position.y + dy * pull
                    )
                }
            }
        }

        // ---- Collision detection -------------------------------------

        checkMissilePlayerCollisions()
        checkMissileMissileCollisions()
        checkPlayerBulletMissileCollisions()
        checkBossPlayerCollisions()
        checkPlayerBulletBossCollisions()
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

        playerBullets.removeAll { b ->
            !b.alive || (
                b.position.x < cameraX - missileMargin ||
                b.position.x > cameraX + screenWidth + missileMargin ||
                b.position.y < cameraY - missileMargin ||
                b.position.y > cameraY + screenHeight + missileMargin
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

        bosses.removeAll { !it.alive }
        explosions.removeAll { !it.alive }
        lottieBlasts.removeAll { !it.alive }

        // ---- Screen shake update ------------------------------------
        if (shakeTimer > 0f) {
            shakeTimer -= dt
            val t = (shakeTimer / Constants.SHAKE_DURATION).coerceIn(0f, 1f)
            shakeOffsetX = (Random.nextFloat() * 2f - 1f) * shakeIntensity * t
            shakeOffsetY = (Random.nextFloat() * 2f - 1f) * shakeIntensity * t
        } else {
            shakeOffsetX = 0f
            shakeOffsetY = 0f
        }

        // ---- Survival score ------------------------------------------

        scoreTimeAccumulator += dt
        if (scoreTimeAccumulator >= 1f) {
            var pts = if (player.doubleScoreActive) 2 else 1
            if (player.ability == PlaneAbility.SCORE_BONUS) {
                pts = (pts * Constants.SCORE_BONUS_MULTIPLIER).toInt().coerceAtLeast(pts)
            }
            score += pts
            scoreTimeAccumulator -= 1f
        }
    }

    // ── Render ─────────────────────────────────────────────────────

    fun render(canvas: Canvas) {
        // Render order: stars -> power-ups -> player bullets -> missiles -> player -> explosions
        for (star in stars) star.render(canvas)
        for (powerUp in powerUps) powerUp.render(canvas)
        for (playerBullet in playerBullets) playerBullet.render(canvas)
        for (missile in missiles) missile.render(canvas)
        for (boss in bosses) boss.render(canvas)
        player.render(canvas)
        for (explosion in explosions) explosion.render(canvas)
        for (blast in lottieBlasts) LottieBlast.renderBlast(canvas, blast)
    }

    // ── Collision helpers ──────────────────────────────────────────

    private fun checkMissilePlayerCollisions() {
        // Index-based loop (not an Iterator) because spawnClusterMinis() below
        // can append new missiles to this same list mid-loop.
        val count = missiles.size
        for (i in 0 until count) {
            val missile = missiles[i]
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
                    spawnShieldShatter(player.position)
                    player.deactivateShield()
                    SoundManager.playShieldHit()
                    triggerShake(Constants.SHAKE_INTENSITY)
                    spawnClusterMinis(missile)
                    // Reset streaks on taking a hit (even with shield)
                    starStreak = 0
                    missileDestroyStreak = 0
                    comboActive = false
                } else if (player.extraLifeAvailable) {
                    // Extra life: survive one fatal hit
                    player.extraLifeAvailable = false
                    missile.alive = false
                    explosions.add(Explosion(missile.position, Constants.MISSILE_COLOR.toInt()))
                    lottieBlasts.add(LottieBlast(missile.position, size = 400f))
                    SoundManager.playShieldHit()
                    triggerShake(Constants.SHAKE_INTENSITY_STRONG)
                    spawnClusterMinis(missile)
                    starStreak = 0
                    missileDestroyStreak = 0
                    comboActive = false
                } else {
                    gameOver = true
                    explosions.add(Explosion(player.position, Constants.MISSILE_COLOR.toInt()))
                    explosions.add(Explosion(player.position, Constants.PLAYER_DEFAULT_COLOR.toInt()))
                    lottieBlasts.add(LottieBlast(player.position, size = 500f))
                    SoundManager.playExplosion()
                    triggerShake(Constants.SHAKE_INTENSITY_STRONG)
                    return
                }
            }
        }
    }

    private fun checkMissileMissileCollisions() {
        if (missiles.size < 2) return

        // Grid-based spatial partitioning to avoid O(n²)
        val cellSize = (Constants.MISSILE_RADIUS * 4f).coerceAtLeast(50f)
        val grid = HashMap<Long, MutableList<Int>>(missiles.size)

        for (i in missiles.indices) {
            val m = missiles[i]
            if (!m.alive) continue
            val cx = (m.position.x / cellSize).toInt()
            val cy = (m.position.y / cellSize).toInt()
            // Insert into this cell and all 8 neighbors isn't needed —
            // just check neighboring cells during collision phase
            val key = cx.toLong() shl 32 or (cy.toLong() and 0xFFFFFFFFL)
            grid.getOrPut(key) { mutableListOf() }.add(i)
        }

        for ((key, indices) in grid) {
            val cx = (key shr 32).toInt()
            val cy = key.toInt()

            // Check within same cell
            for (ii in indices.indices) {
                val a = missiles[indices[ii]]
                if (!a.alive) continue
                for (jj in ii + 1 until indices.size) {
                    val b = missiles[indices[jj]]
                    if (!b.alive) continue
                    if (CollisionUtil.circleCircle(a.position, a.radius, b.position, b.radius)) {
                        resolveMissileCollision(a, b)
                    }
                }
            }

            // Check with 3 neighbor cells (right, bottom, bottom-right) to avoid duplicate pairs
            val neighborOffsets = intArrayOf(1, 0, 0, 1, 1, 1)
            for (n in 0 until 3) {
                val nx = cx + neighborOffsets[n * 2]
                val ny = cy + neighborOffsets[n * 2 + 1]
                val nKey = nx.toLong() shl 32 or (ny.toLong() and 0xFFFFFFFFL)
                val neighborIndices = grid[nKey] ?: continue
                for (ai in indices) {
                    val a = missiles[ai]
                    if (!a.alive) continue
                    for (bi in neighborIndices) {
                        val b = missiles[bi]
                        if (!b.alive) continue
                        if (CollisionUtil.circleCircle(a.position, a.radius, b.position, b.radius)) {
                            resolveMissileCollision(a, b)
                        }
                    }
                }
            }
        }
    }

    private fun resolveMissileCollision(a: Missile, b: Missile) {
        a.alive = false
        b.alive = false
        val mid = (a.position + b.position) * 0.5f
        explosions.add(Explosion(mid, Constants.MISSILE_COLOR.toInt()))
        lottieBlasts.add(LottieBlast(mid, size = 350f))
        score += if (player.doubleScoreActive) 50 else 25
        missilesDestroyed += 2
        SoundManager.playMissileCollide()
        // Cluster missiles spawn mini missiles on death
        spawnClusterMinis(a)
        spawnClusterMinis(b)
        // Missile destroy streak
        missileDestroyStreak += 2
        checkMissileDestroyStreak()
    }

    private fun checkPlayerBulletMissileCollisions() {
        if (playerBullets.isEmpty() || missiles.isEmpty()) return

        // Grid-based spatial partitioning for bullet-missile collisions
        val cellSize = (Constants.MISSILE_RADIUS * 4f).coerceAtLeast(50f)
        val missileGrid = HashMap<Long, MutableList<Int>>(missiles.size)

        for (i in missiles.indices) {
            val m = missiles[i]
            if (!m.alive) continue
            val cx = (m.position.x / cellSize).toInt()
            val cy = (m.position.y / cellSize).toInt()
            val key = cx.toLong() shl 32 or (cy.toLong() and 0xFFFFFFFFL)
            missileGrid.getOrPut(key) { mutableListOf() }.add(i)
        }

        for (bullet in playerBullets) {
            if (!bullet.alive) continue
            val bcx = (bullet.position.x / cellSize).toInt()
            val bcy = (bullet.position.y / cellSize).toInt()

            // Check the bullet's cell and all 8 neighbors
            var hit = false
            for (dx in -1..1) {
                if (hit) break
                for (dy in -1..1) {
                    if (hit) break
                    val key = (bcx + dx).toLong() shl 32 or ((bcy + dy).toLong() and 0xFFFFFFFFL)
                    val indices = missileGrid[key] ?: continue
                    for (mi in indices) {
                        val missile = missiles[mi]
                        if (!missile.alive) continue
                        if (CollisionUtil.circleCircle(
                                bullet.position, bullet.radius,
                                missile.position, missile.radius
                            )
                        ) {
                            bullet.alive = false
                            missile.alive = false
                            explosions.add(Explosion(missile.position, missile.missileColor.toInt()))
                            lottieBlasts.add(LottieBlast(missile.position, size = 300f))
                            score += if (player.doubleScoreActive) 50 else 25
                            missilesDestroyed++
                            SoundManager.playMissileCollide()
                            spawnClusterMinis(missile)
                            missileDestroyStreak++
                            checkMissileDestroyStreak()
                            hit = true
                            break
                        }
                    }
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
                starStreak++
                // Combo: 5 stars in a row = 1.5x score bonus
                comboActive = starStreak >= Constants.STAR_STREAK_THRESHOLD
                var starScore = if (player.doubleScoreActive) 20 else 10
                if (comboActive) {
                    starScore = (starScore * Constants.STAR_STREAK_MULTIPLIER).toInt()
                }
                if (player.ability == PlaneAbility.SCORE_BONUS) {
                    starScore = (starScore * Constants.SCORE_BONUS_MULTIPLIER).toInt()
                }
                score += starScore
                spawnStarSparkle(star.position)
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
                    PowerUpType.SHIELD -> {
                        player.activateShield()
                        shieldsCollected++
                    }
                    PowerUpType.SPEED_BOOST -> player.activateSpeedBoost()
                    PowerUpType.SLOW_MOTION -> player.activateSlowMotion()
                    PowerUpType.MISSILE_JAMMER -> player.activateMissileJammer()
                    PowerUpType.DOUBLE_SCORE -> player.activateDoubleScore()
                    PowerUpType.BULLET_SHOOT -> player.activateBulletShoot()
                }
                SoundManager.playPowerUp()
            }
        }
    }

    // ── Spawners ───────────────────────────────────────────────────

    /**
     * Pick a random missile type based on weighted probabilities.
     * Special types only appear after 15 seconds of survival.
     */
    private fun randomMissileType(): MissileType {
        // Early game: only homing for the first few seconds
        if (survivalTime < 5f) return MissileType.HOMING

        // Weighted distribution: Homing 40%, Straight 20%, Bouncing 15%, Cluster 10%, Stealth 15%
        val roll = Random.nextFloat()
        return when {
            roll < 0.40f -> MissileType.HOMING
            roll < 0.60f -> MissileType.STRAIGHT
            roll < 0.75f -> MissileType.BOUNCING
            roll < 0.85f -> MissileType.CLUSTER
            else -> MissileType.STEALTH
        }
    }

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

        val type = randomMissileType()
        val missile = Missile(
            startPosition = spawnPos,
            initialAngle = initialAngle,
            targetPosition = player.position,
            missileType = type
        )
        // Assign the correct bitmap per missile type
        missile.bulletBitmap = when (type) {
            MissileType.HOMING -> bulletBitmap
            MissileType.STRAIGHT -> missileStraightBitmap
            MissileType.BOUNCING -> missileBouncingBitmap
            MissileType.CLUSTER -> missileClusterBitmap
            MissileType.STEALTH -> missileStealthBitmap
        }
        if (hardMode) {
            missile.speed *= Constants.HARD_MODE_MISSILE_SPEED_MULT
            missile.turnRate *= Constants.HARD_MODE_TURN_RATE_MULT
        }
        missiles.add(missile)
    }

    /**
     * If [missile] is a CLUSTER type, spawn mini homing missiles spreading outward.
     */
    private fun spawnClusterMinis(missile: Missile) {
        if (missile.missileType != MissileType.CLUSTER) return
        if (missile.isClusterMini) return  // minis don't spawn more minis

        val count = Constants.CLUSTER_MINI_COUNT
        for (i in 0 until count) {
            val spreadAngle = missile.angle + (i - count / 2) * (2f * PI.toFloat() / count)
            val mini = Missile(
                startPosition = missile.position,
                initialAngle = spreadAngle,
                targetPosition = player.position,
                missileType = MissileType.HOMING
            )
            mini.isClusterMini = true
            mini.remainingLifetime = Constants.CLUSTER_MINI_LIFETIME
            mini.speed = Constants.CLUSTER_MINI_SPEED
            mini.radius = Constants.CLUSTER_MINI_RADIUS
            missiles.add(mini)
        }
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

    // ── Boss spawning ───────────────────────────────────────────────

    private fun spawnBoss() {
        val viewLeft = cameraX
        val viewTop = cameraY
        val viewRight = cameraX + screenWidth
        val viewBottom = cameraY + screenHeight

        val edge = Random.nextInt(4)
        val margin = Constants.BOSS_RADIUS * 2
        val spawnPos: Vector2 = when (edge) {
            0 -> Vector2(viewLeft + Random.nextFloat() * screenWidth, viewTop - margin)
            1 -> Vector2(viewLeft + Random.nextFloat() * screenWidth, viewBottom + margin)
            2 -> Vector2(viewLeft - margin, viewTop + Random.nextFloat() * screenHeight)
            else -> Vector2(viewRight + margin, viewTop + Random.nextFloat() * screenHeight)
        }

        val dx = player.position.x - spawnPos.x
        val dy = player.position.y - spawnPos.y
        val initialAngle = atan2(dy, dx)

        bosses.add(BossMissile(spawnPos, initialAngle, player.position))
    }

    // ── Boss collision helpers ──────────────────────────────────────

    private fun checkBossPlayerCollisions() {
        for (boss in bosses) {
            if (!boss.alive) continue
            if (CollisionUtil.circleCircle(player.position, player.radius, boss.position, boss.radius)) {
                if (player.shieldActive) {
                    spawnShieldShatter(player.position)
                    player.deactivateShield()
                    SoundManager.playShieldHit()
                    triggerShake(Constants.SHAKE_INTENSITY)
                    // Boss takes a hit from shield impact
                    if (boss.takeHit()) {
                        onBossKilled(boss)
                    } else {
                        explosions.add(Explosion(boss.position, Constants.SHIELD_COLOR.toInt()))
                    }
                    starStreak = 0
                    missileDestroyStreak = 0
                    comboActive = false
                } else if (player.extraLifeAvailable) {
                    player.extraLifeAvailable = false
                    explosions.add(Explosion(boss.position, Constants.BOSS_COLOR.toInt()))
                    lottieBlasts.add(LottieBlast(boss.position, size = 400f))
                    SoundManager.playShieldHit()
                    triggerShake(Constants.SHAKE_INTENSITY_STRONG)
                    starStreak = 0
                    missileDestroyStreak = 0
                    comboActive = false
                } else {
                    gameOver = true
                    explosions.add(Explosion(player.position, Constants.MISSILE_COLOR.toInt()))
                    explosions.add(Explosion(player.position, Constants.PLAYER_DEFAULT_COLOR.toInt()))
                    lottieBlasts.add(LottieBlast(player.position, size = 500f))
                    SoundManager.playExplosion()
                    triggerShake(Constants.SHAKE_INTENSITY_STRONG)
                    return
                }
            }
        }
    }

    private fun checkPlayerBulletBossCollisions() {
        for (bullet in playerBullets) {
            if (!bullet.alive) continue
            for (boss in bosses) {
                if (!boss.alive) continue
                if (CollisionUtil.circleCircle(bullet.position, bullet.radius, boss.position, boss.radius)) {
                    bullet.alive = false
                    explosions.add(Explosion(bullet.position, Constants.BULLET_SHOOT_COLOR.toInt()))
                    SoundManager.playShieldHit()
                    if (boss.takeHit()) {
                        onBossKilled(boss)
                    }
                    break
                }
            }
        }
    }

    private fun onBossKilled(boss: BossMissile) {
        // Big explosion
        explosions.add(Explosion(boss.position, Constants.BOSS_COLOR.toInt()))
        explosions.add(Explosion(boss.position, 0xFFFF5252.toInt()))
        lottieBlasts.add(LottieBlast(boss.position, size = 600f))
        SoundManager.playExplosion()
        triggerShake(Constants.SHAKE_INTENSITY_STRONG)

        // Bonus score
        score += if (player.doubleScoreActive) Constants.BOSS_SCORE_REWARD * 2 else Constants.BOSS_SCORE_REWARD
        missilesDestroyed++

        // Clear all normal missiles as a reward
        for (m in missiles) {
            if (m.alive) {
                m.alive = false
                explosions.add(Explosion(m.position, Constants.MISSILE_COLOR.toInt()))
            }
        }
    }

    // ── Streak helpers ──────────────────────────────────────────────

    private fun checkMissileDestroyStreak() {
        if (missileDestroyStreak >= Constants.MISSILE_DESTROY_STREAK_FOR_SHIELD && !player.shieldActive) {
            player.activateShield()
            missileDestroyStreak = 0
            SoundManager.playPowerUp()
        }
    }

    // ── Screen shake ────────────────────────────────────────────────

    fun triggerShake(intensity: Float = Constants.SHAKE_INTENSITY) {
        shakeTimer = Constants.SHAKE_DURATION
        shakeIntensity = intensity
    }

    // ── Sparkle burst (star collect) ────────────────────────────────

    private fun spawnStarSparkle(pos: Vector2) {
        // Gold sparkle explosion - smaller and faster than regular explosion
        val sparkle = Explosion(pos, Constants.STAR_COLOR.toInt())
        // Make particles smaller and faster-fading for a sparkle feel
        for (p in sparkle.particles) {
            p.radius *= 0.5f
            p.lifetime = Constants.SPARKLE_LIFETIME * (0.5f + Random.nextFloat() * 0.5f)
            p.velocity = p.velocity * 0.6f
        }
        explosions.add(sparkle)
    }

    // ── Shield shatter effect ───────────────────────────────────────

    private fun spawnShieldShatter(pos: Vector2) {
        // Blue shards flying outward
        val shatter = Explosion(pos, Constants.SHIELD_COLOR.toInt())
        for (p in shatter.particles) {
            p.radius = 2f + Random.nextFloat() * 4f
            p.lifetime = Constants.SHIELD_SHATTER_LIFETIME * (0.6f + Random.nextFloat() * 0.4f)
            p.velocity = p.velocity * 1.5f  // faster spread
        }
        explosions.add(shatter)
    }
}
