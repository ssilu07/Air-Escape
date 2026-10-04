package com.royals.airescape.screen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieCompositionFactory
import com.royals.airescape.R
import com.royals.airescape.data.Constants
import com.royals.airescape.data.GameData
import com.royals.airescape.data.PlaneConfig
import com.royals.airescape.engine.GameSurfaceView
import com.royals.airescape.engine.PlayGamesManager
import com.royals.airescape.engine.RatingManager
import com.royals.airescape.engine.Renderer
import com.royals.airescape.engine.SoundManager
import com.royals.airescape.data.DailyChallengeManager
import com.royals.airescape.entity.EntityManager
import com.royals.airescape.entity.LottieBlast
import com.royals.airescape.input.ButtonController
import com.royals.airescape.input.InputManager
import com.royals.airescape.input.JoystickController
import com.royals.airescape.input.TouchController
import com.royals.airescape.ads.AdManager
import com.royals.airescape.util.Vector2
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Core gameplay screen.
 *
 * Sky-blue background with scrolling clouds. Plane icon from drawable.
 * Helicopter sound loops during gameplay.
 */
class GameScreen(private val surfaceView: GameSurfaceView) : Screen {

    // ── Engine pieces ────────────────────────────────────────────────
    private lateinit var entityManager: EntityManager
    private lateinit var inputManager: InputManager

    // ── Screen dimensions ────────────────────────────────────────────
    private var screenWidth = 0f
    private var screenHeight = 0f
    @Volatile
    private var layoutDone = false

    // ── Plane config ─────────────────────────────────────────────────
    private val planeConfig: PlaneConfig
    private val hardMode: Boolean

    // ── Plane bitmap ─────────────────────────────────────────────────
    private var planeBitmap: Bitmap? = null

    // ── Bullet bitmaps ─────────────────────────────────────────────────
    private var bulletBitmap: Bitmap? = null
    private var bulletGreenBitmap: Bitmap? = null
    private var missileStraightBitmap: Bitmap? = null
    private var missileBouncingBitmap: Bitmap? = null
    private var missileClusterBitmap: Bitmap? = null
    private var missileStealthBitmap: Bitmap? = null

    // ── Lottie blast ──────────────────────────────────────────────────
    private var blastLoaded = false

    // ── Helicopter sound ──
    private var helicopterActive = false

    // ── Sky / cloud colors (from selected theme) ─────────────────────
    private val currentTheme: com.royals.airescape.data.ThemeConfig
    private val skyColor: Int
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    init {
        val themeId = GameData.selectedTheme
        currentTheme = com.royals.airescape.data.ThemeConfig.THEMES.firstOrNull { it.id == themeId }
            ?: com.royals.airescape.data.ThemeConfig.THEMES[0]
        skyColor = currentTheme.skyColor.toInt()
        cloudPaint.color = currentTheme.cloudColor.toInt()
        cloudPaint.alpha = currentTheme.cloudAlpha
    }

    // ── HUD paints ───────────────────────────────────────────────────
    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val starHudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Constants.STAR_COLOR.toInt()
        isFakeBoldText = true
        textAlign = Paint.Align.RIGHT
    }

    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF333333.toInt()
        isFakeBoldText = true
        textAlign = Paint.Align.LEFT
    }

    private val barBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF333333.toInt()
        style = Paint.Style.FILL
    }

    private val barFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val barRect = RectF()

    /**
     * Describes one power-up HUD bar. Built each frame from live player state.
     */
    private data class BarInfo(
        val active: Boolean,
        val timer: Float,
        val duration: Float,
        val label: String,
        val expiringLabel: String,
        val color: Long
    )

    // ── Combo / boss HUD paints ────────────────────────────────────────
    private val comboPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Constants.STAR_COLOR.toInt()
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private val bossWarnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF1744.toInt()
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    // ── Tutorial state ────────────────────────────────────────────────
    private var tutorialStep: Int = 0
    private var tutorialTimer: Float = 0f
    private val tutorialMessages = listOf(
        "Drag joystick to steer your plane!",
        "Collect STARS for score",
        "SHIELD (S) blocks one missile hit",
        "SLOW-MO slows all missiles",
        "JAMMER freezes all missiles",
        "SPEED BOOST makes you faster",
        "2X SCORE doubles your points",
        "BULLET fires at missiles automatically"
    )
    private val tutorialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val tutorialBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF000000.toInt()
        alpha = 150
        style = Paint.Style.FILL
    }

    // ── Daily challenge state ──────────────────────────────────────────
    private var challengesSubmitted = false

    // ── Edge arrow paints ─────────────────────────────────────────────
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val arrowPath = android.graphics.Path()

    // ── Pause ─────────────────────────────────────────────────────────
    private var paused = false
    private val pauseButtonRect = RectF()
    private val pauseResumeRect = RectF()
    private val pauseMenuRect = RectF()

    private val pauseIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
    }

    // ── Game-over overlay ───────────────────────────────────────────
    private var showGameOver = false
    private var gameOverDelay = 0f
    private var gameOverAnimTimer = 0f
    private var scoreSubmitted = false
    private var isNewHighScore = false
    private val GAME_OVER_BLAST_DURATION = 0.8f

    private val watchAdButtonRect = RectF()
    private val rateButtonRect = RectF()
    private var adRewardGiven = false
    private var interstitialTriggered = false
    private var ratingTriggered = false

    private val retryButtonRect = RectF()
    private val menuButtonRect = RectF()

    private val overlayPaint = Paint().apply {
        color = 0xFF000000.toInt()
        alpha = 120
        style = Paint.Style.FILL
    }
    private val goTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF4E342E.toInt()
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private val goScorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF3E2723.toInt()
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private val goDetailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF5D4037.toInt()
        textAlign = Paint.Align.CENTER
    }
    private val goHighScorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Constants.STAR_COLOR.toInt()
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private val goDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF8D6E63.toInt()
        strokeWidth = 2f
    }

    init {
        val selectedId = GameData.selectedPlane
        planeConfig = PlaneConfig.PLANES.firstOrNull { it.id == selectedId } ?: PlaneConfig.PLANES[0]
        hardMode = GameData.hardMode
        SoundManager.setSoundEnabled(GameData.soundEnabled)
    }

    // ── Screen interface ─────────────────────────────────────────────

    override fun onEnter() {
        AdManager.hideBanner()
        initGame()
        if (!showGameOver) {
            SoundManager.gameActive = true
            startHelicopterSound()
        }
    }

    override fun onExit() {
        SoundManager.gameActive = false
        stopHelicopterSound()
    }

    override fun onBackPressed(): Boolean {
        surfaceView.setScreen(MenuScreen(surfaceView))
        return true
    }

    override fun update(dt: Float) {
        if (!layoutDone) {
            initGame()
        }
        if (!layoutDone) return

        // Retry loading Lottie blast if it failed during init
        if (!blastLoaded && !LottieBlast.isReady) {
            loadBlastComposition()
        }

        if (paused) return

        val dir = inputManager.getDirection()
        entityManager.player.inputDirection = dir

        entityManager.update(dt)

        // Tutorial progression (first game only)
        if (!GameData.tutorialShown && tutorialStep < tutorialMessages.size) {
            tutorialTimer += dt
            if (tutorialTimer >= 4f) { // 4 seconds per tip
                tutorialTimer = 0f
                tutorialStep++
                if (tutorialStep >= tutorialMessages.size) {
                    GameData.tutorialShown = true
                }
            }
        }

        if (entityManager.gameOver && !showGameOver) {
            gameOverDelay += dt
            if (gameOverDelay >= GAME_OVER_BLAST_DURATION) {
                showGameOver = true
                SoundManager.gameActive = false
                stopHelicopterSound()
                if (!scoreSubmitted) {
                    scoreSubmitted = true
                    isNewHighScore = entityManager.score > GameData.highScore
                    GameData.submitScore(entityManager.score, entityManager.starsCollected)
                    // Submit to Google Play Games leaderboard
                    PlayGamesManager.submitScore(entityManager.score)
                }
                if (!challengesSubmitted) {
                    challengesSubmitted = true
                    DailyChallengeManager.submitGameResult(
                        starsCollected = entityManager.starsCollected,
                        survivalSeconds = entityManager.survivalTime.toInt(),
                        missilesDestroyed = entityManager.missilesDestroyed,
                        shieldsCollected = entityManager.shieldsCollected
                    )
                }
                // Interstitial ad every 3rd game over
                if (!interstitialTriggered) {
                    interstitialTriggered = true
                    AdManager.gameOverCount++
                    if (AdManager.gameOverCount % 3 == 0) {
                        surfaceView.post { AdManager.showInterstitial() }
                    }
                }
                // Auto in-app review (after 5 sessions, if score >= 50)
                if (!ratingTriggered) {
                    ratingTriggered = true
                    surfaceView.post { RatingManager.onGameOver(entityManager.score) }
                }
            }
        }
        if (showGameOver) {
            gameOverAnimTimer += dt
        }
    }

    override fun render(canvas: Canvas) {
        if (!layoutDone) return

        // Sky background
        canvas.drawColor(skyColor)

        // Scrolling clouds
        renderClouds(canvas)

        // Camera transform for world-space entities (with screen shake)
        canvas.save()
        canvas.translate(
            -entityManager.cameraX + entityManager.shakeOffsetX,
            -entityManager.cameraY + entityManager.shakeOffsetY
        )
        entityManager.render(canvas)
        canvas.restore()

        if (!showGameOver) {
            // Edge arrows pointing to off-screen collectibles
            renderEdgeArrows(canvas)

            // Input overlay (screen-space)
            if (!paused) inputManager.render(canvas)

            // HUD (screen-space)
            renderHud(canvas)

            if (paused) renderPauseOverlay(canvas)
        } else {
            // Game-over overlay on top of gameplay
            renderGameOverOverlay(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!layoutDone) return true
        if (paused) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                val x = event.x
                val y = event.y
                if (pauseResumeRect.contains(x, y)) {
                    paused = false
                    SoundManager.gameActive = true
                    startHelicopterSound()
                    return true
                }
                if (pauseMenuRect.contains(x, y)) {
                    surfaceView.setScreen(MenuScreen(surfaceView))
                    return true
                }
            }
            return true
        }
        if (showGameOver) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                val x = event.x
                val y = event.y
                if (watchAdButtonRect.contains(x, y) && AdManager.isRewardedReady() && !adRewardGiven) {
                    surfaceView.post {
                        AdManager.showRewarded(
                            onRewarded = {
                                adRewardGiven = true
                                GameData.totalStars += Constants.REWARD_AD_COINS
                                GameData.addCoins(Constants.REWARD_AD_COINS)
                            }

                        )
                    }
                    return true
                }
                if (retryButtonRect.contains(x, y)) {
                    surfaceView.setScreen(GameScreen(surfaceView))
                    return true
                }
                if (rateButtonRect.contains(x, y)) {
                    surfaceView.post { RatingManager.showReview() }
                    return true
                }
                if (menuButtonRect.contains(x, y)) {
                    surfaceView.setScreen(MenuScreen(surfaceView))
                    return true
                }
            }
            return true
        }
        // Pause button tap
        if (event.action == MotionEvent.ACTION_DOWN && pauseButtonRect.contains(event.x, event.y)) {
            paused = true
            SoundManager.gameActive = false
            stopHelicopterSound()
            return true
        }
        return inputManager.onTouchEvent(event)
    }

    override fun onSizeChanged(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
        if (layoutDone) {
            inputManager.setScreenSize(width, height)
        }
    }

    // ── Initialisation ───────────────────────────────────────────────

    @Synchronized
    private fun initGame() {
        if (layoutDone) return
        val sw = GameSurfaceView.screenWidth.toFloat()
        val sh = GameSurfaceView.screenHeight.toFloat()
        if (sw <= 0f || sh <= 0f) return

        screenWidth = sw
        screenHeight = sh

        entityManager = EntityManager()
        entityManager.init(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            planeType = planeConfig.id % 3,
            planeColor = planeConfig.colorPrimary.toInt(),
            hardMode = hardMode
        )

        // Load plane bitmap
        if (planeBitmap == null) {
            planeBitmap = loadPlaneBitmap()
        }
        entityManager.player.planeBitmap = planeBitmap
        entityManager.player.bitmapRotationOffset = planeConfig.rotationOffset
        entityManager.player.hasBuiltInGun = planeConfig.hasGun
        entityManager.player.ability = planeConfig.ability
        if (planeConfig.ability == com.royals.airescape.data.PlaneAbility.EXTRA_LIFE) {
            entityManager.player.extraLifeAvailable = true
        }

        // Load bullet bitmaps
        if (bulletBitmap == null) {
            bulletBitmap = loadBulletBitmap(R.drawable.bullet_red)
        }
        entityManager.bulletBitmap = bulletBitmap
        if (bulletGreenBitmap == null) {
            bulletGreenBitmap = loadBulletBitmap(R.drawable.bullet_green)
        }
        entityManager.bulletGreenBitmap = bulletGreenBitmap

        // Load missile-type bitmaps
        if (missileStraightBitmap == null) {
            missileStraightBitmap = loadBulletBitmap(R.drawable.missile_straight)
        }
        entityManager.missileStraightBitmap = missileStraightBitmap
        if (missileBouncingBitmap == null) {
            missileBouncingBitmap = loadBulletBitmap(R.drawable.missile_bouncing)
        }
        entityManager.missileBouncingBitmap = missileBouncingBitmap
        if (missileClusterBitmap == null) {
            missileClusterBitmap = loadBulletBitmap(R.drawable.missile_cluster)
        }
        entityManager.missileClusterBitmap = missileClusterBitmap
        if (missileStealthBitmap == null) {
            missileStealthBitmap = loadBulletBitmap(R.drawable.missile_stealth)
        }
        entityManager.missileStealthBitmap = missileStealthBitmap

        // Load Lottie blast composition
        if (!blastLoaded) {
            loadBlastComposition()
        }

        inputManager = when (GameData.controlType) {
            1 -> TouchController()
            2 -> ButtonController()
            else -> JoystickController()
        }
        inputManager.setScreenSize(screenWidth, screenHeight)

        layoutDone = true
    }

    private fun loadPlaneBitmap(): Bitmap? {
        return try {
            val drawable = ContextCompat.getDrawable(surfaceView.context, planeConfig.drawableRes)
                ?: return null
            val size = (Constants.PLAYER_RADIUS * 4f).toInt()
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c = Canvas(bitmap)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(c)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun loadBulletBitmap(drawableRes: Int = R.drawable.bullet_red): Bitmap? {
        return try {
            val drawable = ContextCompat.getDrawable(surfaceView.context, drawableRes)
                ?: return null
            val size = (Constants.MISSILE_RADIUS * 4f).toInt()
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c = Canvas(bitmap)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(c)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun loadBlastComposition() {
        try {
            val result = LottieCompositionFactory.fromAssetSync(surfaceView.context, "blast.json")
            val composition = result.value ?: return
            LottieBlast.preRenderFrames(composition)
            blastLoaded = true
        } catch (_: Exception) {
        }
    }

    // ── Helicopter sound ─────────────────────────────────────────────

    // ── Game-over overlay ────────────────────────────────────────────

    private fun renderGameOverOverlay(canvas: Canvas) {
        // Semi-transparent overlay (light so sky background shows through)
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, overlayPaint)

        val cx = screenWidth / 2f

        // "YOUR SCORE" title
        goTitlePaint.textSize = screenWidth * 0.09f
        canvas.drawText("YOUR SCORE", cx, screenHeight * 0.25f, goTitlePaint)

        // Big score number
        goScorePaint.textSize = screenWidth * 0.18f
        canvas.drawText("${entityManager.score}", cx, screenHeight * 0.36f, goScorePaint)

        // Stars + Coins collected (1 Star = 1 Coin)
        goDetailPaint.textSize = screenWidth * 0.042f
        val starText = "\u2605 +${entityManager.starsCollected} Stars  |  \uD83E\uDE99 +${entityManager.starsCollected} Coins"
        canvas.drawText(starText, cx, screenHeight * 0.44f, goDetailPaint)

        // Survival time
        val totalSec = entityManager.survivalTime.toInt()
        val min = totalSec / 60
        val sec = totalSec % 60
        val timeText = String.format("\u23F1  +%d:%02d", min, sec)
        canvas.drawText(timeText, cx, screenHeight * 0.49f, goDetailPaint)

        // Divider line
        val divW = screenWidth * 0.45f
        canvas.drawLine(cx - divW / 2f, screenHeight * 0.53f, cx + divW / 2f, screenHeight * 0.53f, goDividerPaint)

        // Total stars & Wallet balance
        goDetailPaint.textSize = screenWidth * 0.038f
        val walletRs = String.format(java.util.Locale.US, "%.2f", GameData.totalCoins / Constants.COINS_PER_RUPEE.toFloat())
        val totalsText = "\u2605 ${GameData.totalStars} Stars  \u2022  \uD83E\uDE99 ${GameData.totalCoins} Coins (\u20B9$walletRs)"
        canvas.drawText(totalsText, cx, screenHeight * 0.575f, goDetailPaint)


        // New high score
        if (isNewHighScore) {
            val pulse = 0.7f + 0.3f * sin(gameOverAnimTimer * 4f).toFloat()
            goHighScorePaint.textSize = screenWidth * 0.055f
            goHighScorePaint.alpha = (pulse * 255).toInt().coerceIn(0, 255)
            canvas.drawText("NEW HIGH SCORE!", cx, screenHeight * 0.63f, goHighScorePaint)
        }

        // ── Daily Challenges ─────────────────────────────────────────
        val challenges = DailyChallengeManager.getTodayChallenges()
        if (challenges.isNotEmpty()) {
            var challengeY = screenHeight * 0.60f
            goDetailPaint.textSize = screenWidth * 0.032f
            goDetailPaint.textAlign = Paint.Align.CENTER

            Renderer.drawText(
                canvas, "DAILY CHALLENGES",
                cx, challengeY,
                size = screenWidth * 0.035f,
                color = Constants.STAR_COLOR,
                align = Paint.Align.CENTER
            )
            challengeY += screenWidth * 0.045f

            for (challenge in challenges) {
                val check = if (challenge.isCompleted) "\u2713 " else ""
                val text = "$check${challenge.description} (${challenge.progress}/${challenge.target})"
                val color = if (challenge.isCompleted) 0xFF00E676 else 0xFF5D4037
                Renderer.drawText(
                    canvas, text,
                    cx, challengeY,
                    size = screenWidth * 0.028f,
                    color = color,
                    align = Paint.Align.CENTER
                )
                if (challenge.isCompleted) {
                    Renderer.drawText(
                        canvas, "+${challenge.rewardStars}\u2605",
                        cx + screenWidth * 0.35f, challengeY,
                        size = screenWidth * 0.025f,
                        color = Constants.STAR_COLOR,
                        align = Paint.Align.LEFT
                    )
                }
                challengeY += screenWidth * 0.04f
            }
        }

        // Layout buttons
        val btnW = screenWidth * 0.7f
        val btnH = screenHeight * 0.055f
        val gap = screenHeight * 0.018f

        val showWatchAd = AdManager.isRewardedReady() && !adRewardGiven
        var nextTop = screenHeight * 0.78f

        // Watch Ad button (gold) - only if rewarded ad available and not yet claimed
        if (showWatchAd) {
            watchAdButtonRect.set(cx - btnW / 2f, nextTop, cx + btnW / 2f, nextTop + btnH)
            Renderer.drawButton(canvas, watchAdButtonRect, "WATCH AD  +${Constants.REWARD_AD_COINS} \u2605 & +${Constants.REWARD_AD_COINS} \uD83E\uDE99",
                color = 0xFFFFD600, textColor = 0xFF1A1A2E)
            nextTop += btnH + gap
        }


        // Retry button (green)
        retryButtonRect.set(cx - btnW / 2f, nextTop, cx + btnW / 2f, nextTop + btnH)
        Renderer.drawButton(canvas, retryButtonRect, "RETRY", color = 0xFF00E676, textColor = 0xFF1A1A2E)

        nextTop += btnH + gap

        // Rate Us button (orange-amber)
        rateButtonRect.set(cx - btnW / 2f, nextTop, cx + btnW / 2f, nextTop + btnH)
        Renderer.drawButton(canvas, rateButtonRect, "\u2B50 RATE US", color = 0xFFFF9800, textColor = 0xFF1A1A2E)

        nextTop += btnH + gap

        // Menu button (gray)
        menuButtonRect.set(cx - btnW / 2f, nextTop, cx + btnW / 2f, nextTop + btnH)
        Renderer.drawButton(canvas, menuButtonRect, "MENU", color = 0xFF78909C, textColor = 0xFFFFFFFF)
    }

    private fun startHelicopterSound() {
        if (!GameData.soundEnabled) return
        helicopterActive = true
        // Decode on first use (cached after that)
        SoundManager.decodeHelicopterSound(surfaceView.context, R.raw.helicopter)
        SoundManager.startHelicopterLoop()
    }

    private fun stopHelicopterSound() {
        helicopterActive = false
        SoundManager.stopHelicopterLoop()
    }

    // ── Edge arrows for off-screen collectibles ─────────────────────

    private fun renderEdgeArrows(canvas: Canvas) {
        val camX = entityManager.cameraX
        val camY = entityManager.cameraY
        val margin = 35f  // distance from screen edge
        val arrowSize = 28f

        // Stars (gold arrows)
        for (star in entityManager.stars) {
            if (!star.alive) continue
            val sx = star.position.x - camX
            val sy = star.position.y - camY
            if (sx in 0f..screenWidth && sy in 0f..screenHeight) continue // on screen
            drawEdgeArrow(canvas, sx, sy, margin, arrowSize, Constants.STAR_COLOR.toInt(), 200)
        }

        // Power-ups (shield = blue, boost = orange)
        for (pu in entityManager.powerUps) {
            if (!pu.alive) continue
            val sx = pu.position.x - camX
            val sy = pu.position.y - camY
            if (sx in 0f..screenWidth && sy in 0f..screenHeight) continue
            val color = when (pu.type) {
                com.royals.airescape.entity.PowerUpType.SHIELD -> Constants.SHIELD_COLOR.toInt()
                com.royals.airescape.entity.PowerUpType.SPEED_BOOST -> Constants.SPEED_BOOST_COLOR.toInt()
                com.royals.airescape.entity.PowerUpType.SLOW_MOTION -> Constants.SLOW_MOTION_COLOR.toInt()
                com.royals.airescape.entity.PowerUpType.MISSILE_JAMMER -> Constants.MISSILE_JAMMER_COLOR.toInt()
                com.royals.airescape.entity.PowerUpType.DOUBLE_SCORE -> Constants.DOUBLE_SCORE_COLOR.toInt()
                com.royals.airescape.entity.PowerUpType.BULLET_SHOOT -> Constants.BULLET_SHOOT_COLOR.toInt()
            }
            drawEdgeArrow(canvas, sx, sy, margin, arrowSize, color, 200)
        }
    }

    /**
     * Draw a small triangle arrow at the screen edge pointing toward (sx, sy).
     */
    private fun drawEdgeArrow(
        canvas: Canvas, sx: Float, sy: Float,
        margin: Float, size: Float, color: Int, alpha: Int
    ) {
        val cx = screenWidth / 2f
        val cy = screenHeight / 2f
        val angle = atan2(sy - cy, sx - cx)

        // Clamp to screen edge
        val edgeX = sx.coerceIn(margin, screenWidth - margin)
        val edgeY = sy.coerceIn(margin, screenHeight - margin)

        arrowPaint.color = color
        arrowPaint.alpha = alpha

        // Triangle pointing in direction of the item
        val cosA = cos(angle)
        val sinA = sin(angle)
        val tipX = edgeX + cosA * size
        val tipY = edgeY + sinA * size
        val perpX = -sinA
        val perpY = cosA
        val baseL_X = edgeX - cosA * size * 0.5f + perpX * size * 0.6f
        val baseL_Y = edgeY - sinA * size * 0.5f + perpY * size * 0.6f
        val baseR_X = edgeX - cosA * size * 0.5f - perpX * size * 0.6f
        val baseR_Y = edgeY - sinA * size * 0.5f - perpY * size * 0.6f

        arrowPath.reset()
        arrowPath.moveTo(tipX, tipY)
        arrowPath.lineTo(baseL_X, baseL_Y)
        arrowPath.lineTo(baseR_X, baseR_Y)
        arrowPath.close()
        canvas.drawPath(arrowPath, arrowPaint)
    }

    // ── Scrolling clouds ─────────────────────────────────────────────

    private fun renderClouds(canvas: Canvas) {
        val camX = entityManager.cameraX
        val camY = entityManager.cameraY
        val gridSize = 400f

        val startCol = (camX / gridSize).toInt() - 1
        val startRow = (camY / gridSize).toInt() - 1
        val endCol = ((camX + screenWidth) / gridSize).toInt() + 1
        val endRow = ((camY + screenHeight) / gridSize).toInt() + 1

        for (col in startCol..endCol) {
            for (row in startRow..endRow) {
                val hash = (((col.toLong() * 73856093L + row.toLong() * 19349669L) and 0x7FFFFFFF) % 100).toInt()
                if (hash < 28) {
                    val offsetX = ((hash * 37) % gridSize.toInt()).toFloat()
                    val offsetY = ((hash * 53) % gridSize.toInt()).toFloat()
                    val worldX = col * gridSize + offsetX
                    val worldY = row * gridSize + offsetY
                    val screenX = worldX - camX
                    val screenY = worldY - camY
                    val size = 45f + (hash % 4) * 18f
                    drawCloud(canvas, screenX, screenY, size)
                }
            }
        }
    }

    private fun drawCloud(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        canvas.drawCircle(cx, cy, size, cloudPaint)
        canvas.drawCircle(cx - size * 0.7f, cy + size * 0.15f, size * 0.65f, cloudPaint)
        canvas.drawCircle(cx + size * 0.7f, cy + size * 0.15f, size * 0.65f, cloudPaint)
        canvas.drawCircle(cx - size * 0.35f, cy - size * 0.3f, size * 0.55f, cloudPaint)
        canvas.drawCircle(cx + size * 0.35f, cy - size * 0.3f, size * 0.55f, cloudPaint)
    }

    // ── HUD rendering ────────────────────────────────────────────────

    private fun renderHud(canvas: Canvas) {
        val padding = screenWidth * 0.03f
        val hudTextSize = screenWidth * 0.045f
        val smallTextSize = screenWidth * 0.032f

        // Score (top center)
        scorePaint.textSize = hudTextSize * 1.3f
        canvas.drawText(
            "${entityManager.score}",
            screenWidth / 2f, padding + hudTextSize,
            scorePaint
        )

        // Stars collected (top right with star icon)
        starHudPaint.textSize = smallTextSize
        val starText = "${entityManager.starsCollected}"
        val starTextX = screenWidth - padding
        val starTextY = padding + smallTextSize

        val starIconX = starTextX - starHudPaint.measureText(starText) - smallTextSize * 0.8f
        Renderer.drawStar(
            canvas,
            Vector2(starIconX, starTextY - smallTextSize * 0.3f),
            smallTextSize * 0.45f,
            5,
            Constants.STAR_COLOR
        )
        canvas.drawText(starText, starTextX, starTextY, starHudPaint)

        // Coins earned (below star count: 1 Star = 1 Coin)
        val coinTextY = starTextY + smallTextSize * 1.3f
        val coinText = "+${entityManager.starsCollected}"
        val coinIconX = starTextX - starHudPaint.measureText(coinText) - smallTextSize * 0.8f
        Renderer.drawCoin(
            canvas,
            Vector2(coinIconX, coinTextY - smallTextSize * 0.3f),
            smallTextSize * 0.42f
        )
        canvas.drawText(coinText, starTextX, coinTextY, starHudPaint)


        // Survival time (top left)
        val totalSeconds = entityManager.survivalTime.toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val timeStr = String.format("%d:%02d", minutes, seconds)
        timePaint.textSize = smallTextSize
        canvas.drawText(timeStr, padding, padding + smallTextSize, timePaint)

        // Pause button (top-left, below time)
        val pauseSize = hudTextSize * 1.2f
        val pauseX = padding
        val pauseY = padding + smallTextSize + padding * 0.5f
        pauseButtonRect.set(pauseX, pauseY, pauseX + pauseSize, pauseY + pauseSize)
        // Draw pause icon (two vertical bars)
        val barW = pauseSize * 0.25f
        val barH = pauseSize * 0.7f
        val barTop = pauseY + (pauseSize - barH) / 2f
        pauseIconPaint.color = 0xFFFFFFFF.toInt()
        pauseIconPaint.alpha = 200
        canvas.drawRoundRect(
            RectF(pauseX + pauseSize * 0.15f, barTop, pauseX + pauseSize * 0.15f + barW, barTop + barH),
            3f, 3f, pauseIconPaint
        )
        canvas.drawRoundRect(
            RectF(pauseX + pauseSize * 0.6f, barTop, pauseX + pauseSize * 0.6f + barW, barTop + barH),
            3f, 3f, pauseIconPaint
        )

        // Power-up timer bars (data-driven, stacked below score)
        val barWidth = screenWidth * 0.3f
        val barHeight = screenHeight * 0.012f
        val barX = (screenWidth - barWidth) / 2f
        val barSpacing = screenHeight * 0.04f

        val warnTime = Constants.POWERUP_WARN_TIME
        val gameTime = entityManager.survivalTime
        val p = entityManager.player

        val bars = listOf(
            BarInfo(p.shieldActive, Float.MAX_VALUE, Float.MAX_VALUE, "SHIELD", "SHIELD", Constants.SHIELD_COLOR),
            BarInfo(p.speedBoostActive, p.speedBoostTimer, Constants.SPEED_BOOST_DURATION, "BOOST", "BOOST ENDING!", Constants.SPEED_BOOST_COLOR),
            BarInfo(p.slowMotionActive, p.slowMotionTimer, Constants.SLOW_MOTION_DURATION, "SLOW-MO", "SLOW-MO ENDING!", Constants.SLOW_MOTION_COLOR),
            BarInfo(p.missileJammerActive, p.missileJammerTimer, Constants.MISSILE_JAMMER_DURATION, "JAMMER", "JAMMER ENDING!", Constants.MISSILE_JAMMER_COLOR),
            BarInfo(p.doubleScoreActive, p.doubleScoreTimer, Constants.DOUBLE_SCORE_DURATION, "2X SCORE", "2X ENDING!", Constants.DOUBLE_SCORE_COLOR),
            BarInfo(p.bulletShootActive, p.bulletShootTimer, Constants.BULLET_SHOOT_DURATION, "BULLET", "BULLET ENDING!", Constants.BULLET_SHOOT_COLOR)
        )

        var barIndex = 0
        for (bar in bars) {
            if (!bar.active) continue
            val barY = padding + hudTextSize * 1.8f + barIndex * barSpacing
            val expiring = bar.timer in 0f..warnTime && bar.duration != Float.MAX_VALUE
            val flashVisible = !expiring || (gameTime * 8f).toInt() % 2 == 0

            barRect.set(barX, barY, barX + barWidth, barY + barHeight)
            canvas.drawRoundRect(barRect, 4f, 4f, barBgPaint)

            if (flashVisible) {
                val fill = if (bar.duration == Float.MAX_VALUE) 1f
                           else (bar.timer / bar.duration).coerceIn(0f, 1f)
                barFillPaint.color = bar.color.toInt()
                barRect.set(barX, barY, barX + barWidth * fill, barY + barHeight)
                canvas.drawRoundRect(barRect, 4f, 4f, barFillPaint)
            }
            Renderer.drawText(
                canvas, if (expiring) bar.expiringLabel else bar.label,
                screenWidth / 2f, barY - 4f,
                size = smallTextSize * 0.7f,
                color = bar.color,
                align = Paint.Align.CENTER
            )
            barIndex++
        }

        // ── Combo / streak display ──────────────────────────────────
        if (entityManager.comboActive) {
            comboPaint.textSize = smallTextSize * 1.1f
            val comboText = "COMBO x${entityManager.starStreak} (1.5x)"
            val pulseFactor = 0.8f + 0.2f * sin(entityManager.survivalTime * 6f).toFloat()
            comboPaint.alpha = (pulseFactor * 255).toInt().coerceIn(0, 255)
            canvas.drawText(comboText, screenWidth / 2f, screenHeight * 0.15f, comboPaint)
        }

        // Missile destroy streak indicator
        if (entityManager.missileDestroyStreak > 0) {
            val streakText = "Destroy streak: ${entityManager.missileDestroyStreak}/${Constants.MISSILE_DESTROY_STREAK_FOR_SHIELD}"
            Renderer.drawText(
                canvas, streakText,
                screenWidth / 2f, screenHeight * 0.18f,
                size = smallTextSize * 0.7f,
                color = Constants.SHIELD_COLOR,
                align = Paint.Align.CENTER
            )
        }

        // ── Boss warning / HP ───────────────────────────────────────
        if (entityManager.bossAlive) {
            bossWarnPaint.textSize = hudTextSize
            val pulse = 0.6f + 0.4f * sin(entityManager.survivalTime * 5f).toFloat()
            bossWarnPaint.alpha = (pulse * 255).toInt().coerceIn(0, 255)
            canvas.drawText("BOSS!", screenWidth / 2f, screenHeight * 0.22f, bossWarnPaint)
        }

        // ── Tutorial tooltips (first game only) ─────────────────────
        if (!GameData.tutorialShown && tutorialStep < tutorialMessages.size) {
            val msg = tutorialMessages[tutorialStep]
            tutorialPaint.textSize = screenWidth * 0.035f

            val tooltipY = screenHeight * 0.88f
            val tooltipW = screenWidth * 0.8f
            val tooltipH = screenWidth * 0.07f

            // Background pill
            val bgRect = RectF(
                screenWidth / 2f - tooltipW / 2f,
                tooltipY - tooltipH / 2f,
                screenWidth / 2f + tooltipW / 2f,
                tooltipY + tooltipH / 2f
            )
            canvas.drawRoundRect(bgRect, 16f, 16f, tutorialBgPaint)

            // Fade in/out
            val fadeIn = (tutorialTimer / 0.5f).coerceIn(0f, 1f)
            val fadeOut = if (tutorialTimer > 3.5f) 1f - ((tutorialTimer - 3.5f) / 0.5f).coerceIn(0f, 1f) else 1f
            tutorialPaint.alpha = (fadeIn * fadeOut * 255).toInt().coerceIn(0, 255)

            canvas.drawText(msg, screenWidth / 2f, tooltipY + tutorialPaint.textSize / 3f, tutorialPaint)
        }
    }

    // ── Pause overlay ─────────────────────────────────────────────────

    private fun renderPauseOverlay(canvas: Canvas) {
        // Dim background
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, overlayPaint)

        val cx = screenWidth / 2f

        // "PAUSED" title
        goTitlePaint.textSize = screenWidth * 0.1f
        canvas.drawText("PAUSED", cx, screenHeight * 0.38f, goTitlePaint)

        // Buttons
        val btnW = screenWidth * 0.6f
        val btnH = screenHeight * 0.06f
        val gap = screenHeight * 0.02f

        val resumeTop = screenHeight * 0.48f
        pauseResumeRect.set(cx - btnW / 2f, resumeTop, cx + btnW / 2f, resumeTop + btnH)
        Renderer.drawButton(canvas, pauseResumeRect, "RESUME", color = 0xFF00E676, textColor = 0xFF1A1A2E)

        val menuTop = resumeTop + btnH + gap
        pauseMenuRect.set(cx - btnW / 2f, menuTop, cx + btnW / 2f, menuTop + btnH)
        Renderer.drawButton(canvas, pauseMenuRect, "MENU", color = 0xFF78909C, textColor = 0xFFFFFFFF)
    }
}
