package com.royals.airescape.screen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
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
class GameScreen(
    private val surfaceView: GameSurfaceView,
    val levelConfig: com.royals.airescape.level.LevelConfig? = null
) : Screen {

    val effectiveLevelConfig: com.royals.airescape.level.LevelConfig? =
        levelConfig ?: if (GameData.isLevelMode) com.royals.airescape.level.LevelManager.getLevel(GameData.selectedLevel) else null

    private var winDelay = 0f

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
        val themeId = effectiveLevelConfig?.themeId ?: GameData.selectedTheme
        currentTheme = com.royals.airescape.data.ThemeConfig.THEMES.firstOrNull { it.id == themeId }
            ?: com.royals.airescape.data.ThemeConfig.THEMES[0]
        skyColor = currentTheme.skyColor.toInt()
        cloudPaint.color = currentTheme.cloudColor.toInt()
        cloudPaint.alpha = currentTheme.cloudAlpha
    }

    // ── Dynamic Background Shaders ────────────────────────────────────
    private val bgGradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lastBgHeight = 0f
    private var bgShader: LinearGradient? = null

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
        if (effectiveLevelConfig != null) {
            surfaceView.setScreen(LevelSelectScreen(surfaceView))
        } else {
            if (effectiveLevelConfig != null) surfaceView.setScreen(LevelSelectScreen(surfaceView)) else surfaceView.setScreen(MenuScreen(surfaceView))
        }
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

        if (entityManager.levelWon) {
            winDelay += dt
            if (winDelay >= 0.75f && effectiveLevelConfig != null) {
                SoundManager.gameActive = false
                stopHelicopterSound()
                surfaceView.setScreen(
                    LevelCompleteScreen(
                        surfaceView,
                        effectiveLevelConfig,
                        entityManager.starsCollected,
                        entityManager.score,
                        !entityManager.playerHitTaken,
                        entityManager.survivalTime
                    )
                )
                return
            }
        }

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

        // Dynamic gradient sky background
        renderBackground(canvas)

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
                    if (effectiveLevelConfig != null) surfaceView.setScreen(LevelSelectScreen(surfaceView)) else surfaceView.setScreen(MenuScreen(surfaceView))
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
                    surfaceView.setScreen(GameScreen(surfaceView, levelConfig = effectiveLevelConfig))
                    return true
                }
                if (rateButtonRect.contains(x, y)) {
                    surfaceView.post { RatingManager.showReview() }
                    return true
                }
                if (menuButtonRect.contains(x, y)) {
                    if (effectiveLevelConfig != null) surfaceView.setScreen(LevelSelectScreen(surfaceView)) else surfaceView.setScreen(MenuScreen(surfaceView))
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
            hardMode = hardMode,
            levelConfig = effectiveLevelConfig
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
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, overlayPaint)
        val cx = screenWidth / 2f
        val enter = (gameOverAnimTimer / 0.45f).coerceIn(0f, 1f)
        val ease = 1f - (1f - enter) * (1f - enter)

        // ── Result card ──────────────────────────────────────────────
        val cardW = screenWidth * 0.88f
        val cardTop = screenHeight * 0.10f + (1f - ease) * screenHeight * 0.08f
        val cardBottom = screenHeight * 0.60f + (1f - ease) * screenHeight * 0.08f
        val card = RectF(cx - cardW / 2f, cardTop, cx + cardW / 2f, cardBottom)
        val accent = if (isNewHighScore) 0xFFFFD700.toInt() else 0xFF00E5FF.toInt()
        canvas.save()
        canvas.saveLayerAlpha(0f, 0f, screenWidth, screenHeight, (ease * 255).toInt())
        Renderer.drawGlassPanel(canvas, card, accent, 32f)
        val ch = card.height()

        if (effectiveLevelConfig != null) {
            val pct = (entityManager.levelProgress * 100).toInt()
            val curM = (effectiveLevelConfig.distanceMeters * entityManager.levelProgress).toInt()
            Renderer.drawShadowText(canvas, "LEVEL FAILED ($pct%)", cx, card.top + ch * 0.08f, screenWidth * 0.055f, 0xFFFF5252.toInt(), Paint.Align.CENTER)
            Renderer.drawShadowText(canvas, "Reached $curM / ${effectiveLevelConfig.distanceMeters}m", cx, card.top + ch * 0.13f, screenWidth * 0.032f, 0xFFFFB74D.toInt(), Paint.Align.CENTER)
        } else {
            Renderer.drawShadowText(canvas, "MISSION OVER", cx, card.top + ch * 0.11f, screenWidth * 0.05f, 0xFFFF8A80.toInt(), Paint.Align.CENTER)
        }

        // Big score with count-up
        val shown = (entityManager.score * ease).toInt()
        Renderer.drawShadowText(canvas, "$shown", cx, card.top + ch * 0.30f, screenWidth * 0.17f, 0xFFFFFFFF.toInt(), Paint.Align.CENTER)
        Renderer.drawShadowText(canvas, "SCORE", cx, card.top + ch * 0.37f, screenWidth * 0.032f, 0xFF7FDBFF.toInt(), Paint.Align.CENTER)

        if (isNewHighScore) {
            val pulse = 0.75f + 0.25f * sin(gameOverAnimTimer * 5f).toFloat()
            val badgeW = screenWidth * 0.5f
            val badgeH = screenWidth * 0.075f
            val by = card.top + ch * 0.46f
            val badge = RectF(cx - badgeW / 2f, by - badgeH / 2f, cx + badgeW / 2f, by + badgeH / 2f)
            Renderer.drawGlassPanel(canvas, badge, 0xFFFFD700.toInt(), badgeH / 2f, 0xFF6B4E00.toInt(), 0xFF3A2A00.toInt())
            Renderer.drawShadowText(canvas, "\uD83C\uDFC6 NEW HIGH SCORE", cx, by + badgeH * 0.18f, badgeH * 0.45f, 0xFFFFD700.toInt(), Paint.Align.CENTER, (pulse * 255).toInt())
        } else {
            Renderer.drawShadowText(canvas, "\uD83C\uDFC6 BEST ${GameData.highScore}", cx, card.top + ch * 0.47f, screenWidth * 0.04f, 0xFFFFD700.toInt(), Paint.Align.CENTER)
        }

        // Stat tiles: stars | coins | time
        val totalSec = entityManager.survivalTime.toInt()
        val stats = listOf(
            Triple("\u2605", "${entityManager.starsCollected}", "STARS"),
            Triple("\uD83E\uDE99", "+${entityManager.starsCollected}", "COINS"),
            Triple("\u23F1", String.format("%d:%02d", totalSec / 60, totalSec % 60), "TIME")
        )
        val tileGap = screenWidth * 0.02f
        val tileW = (cardW - tileGap * 4f) / 3f
        val tileH = ch * 0.2f
        val tileTop = card.top + ch * 0.56f
        stats.forEachIndexed { i, (icon, value, label) ->
            val l = card.left + tileGap + i * (tileW + tileGap)
            val tile = RectF(l, tileTop, l + tileW, tileTop + tileH)
            Renderer.drawGlassPanel(canvas, tile, 0x55FFFFFF, 16f, 0x66233358, 0x66101A33)
            Renderer.drawShadowText(canvas, "$icon $value", tile.centerX(), tile.top + tileH * 0.52f, screenWidth * 0.043f, 0xFFFFFFFF.toInt(), Paint.Align.CENTER)
            Renderer.drawShadowText(canvas, label, tile.centerX(), tile.bottom - tileH * 0.16f, screenWidth * 0.024f, 0xFF90A4AE.toInt(), Paint.Align.CENTER)
        }

        // Wallet line
        val walletRs = String.format(java.util.Locale.US, "%.2f", GameData.totalCoins / Constants.COINS_PER_RUPEE.toFloat())
        Renderer.drawShadowText(
            canvas, "Wallet  \uD83E\uDE99 ${GameData.totalCoins}  (\u20B9$walletRs)",
            cx, card.top + ch * 0.90f, screenWidth * 0.036f, 0xFFB0BEC5.toInt(), Paint.Align.CENTER
        )

        // ── Daily challenges ─────────────────────────────────────────
        val challenges = DailyChallengeManager.getTodayChallenges()
        var nextTop = card.bottom + screenHeight * 0.02f
        if (challenges.isNotEmpty()) {
            val rowH = screenWidth * 0.075f
            val panelH = rowH * (challenges.size + 1) + 8f
            val panel = RectF(cx - cardW / 2f, nextTop, cx + cardW / 2f, nextTop + panelH)
            Renderer.drawGlassPanel(canvas, panel, 0xFFFFD700.toInt(), 20f)
            Renderer.drawShadowText(canvas, "DAILY CHALLENGES", cx, panel.top + rowH * 0.75f, screenWidth * 0.031f, 0xFFFFD700.toInt(), Paint.Align.CENTER)
            challenges.forEachIndexed { i, c ->
                val y = panel.top + rowH * (i + 1) + rowH * 0.7f
                val col = if (c.isCompleted) 0xFF00E676.toInt() else 0xFFE0E0E0.toInt()
                val mark = if (c.isCompleted) "\u2713 " else ""
                Renderer.drawShadowText(canvas, "$mark${c.description}", panel.left + 16f, y, screenWidth * 0.027f, col)
                val frac = (c.progress.toFloat() / c.target.coerceAtLeast(1)).coerceIn(0f, 1f)
                val bar = RectF(panel.right - screenWidth * 0.26f, y - screenWidth * 0.014f, panel.right - 16f - screenWidth * 0.09f, y - screenWidth * 0.004f + screenWidth * 0.004f)
                Renderer.drawProgressPill(canvas, bar, frac, if (c.isCompleted) 0xFF00E676.toInt() else 0xFF00B0FF.toInt())
                Renderer.drawShadowText(canvas, if (c.isCompleted) "+${c.rewardStars}\u2605" else "${c.progress}/${c.target}", panel.right - 14f, y, screenWidth * 0.026f, if (c.isCompleted) 0xFFFFD700.toInt() else 0xFF90A4AE.toInt(), Paint.Align.RIGHT)
            }
            nextTop = panel.bottom + screenHeight * 0.02f
        }
        canvas.restore()
        canvas.restore()

        // ── Buttons ──────────────────────────────────────────────────
        val btnW = screenWidth * 0.78f
        val btnH = screenHeight * 0.058f
        val gap = screenHeight * 0.017f
        val showWatchAd = AdManager.isRewardedReady() && !adRewardGiven
        val bottomLimit = screenHeight - 0f - gap
        var top = nextTop.coerceAtMost(bottomLimit - (btnH + gap) * (if (showWatchAd) 4 else 3))

        if (showWatchAd) {
            watchAdButtonRect.set(cx - btnW / 2f, top, cx + btnW / 2f, top + btnH)
            Renderer.drawGlossyButton(canvas, watchAdButtonRect, "\uD83C\uDFAC WATCH AD  +${Constants.REWARD_AD_COINS} \u2605 +${Constants.REWARD_AD_COINS} \uD83E\uDE99", 0xFFFFC400.toInt(), 0xFF2B1B00.toInt())
            top += btnH + gap
        }
        retryButtonRect.set(cx - btnW / 2f, top, cx + btnW / 2f, top + btnH)
        Renderer.drawGlossyButton(canvas, retryButtonRect, "\u21BB  RETRY", 0xFF00E676.toInt(), 0xFF06210F.toInt())
        top += btnH + gap
        rateButtonRect.set(cx - btnW / 2f, top, cx + btnW / 2f, top + btnH)
        Renderer.drawGlossyButton(canvas, rateButtonRect, "\u2B50 RATE US", 0xFFFF9800.toInt(), 0xFF2B1600.toInt())
        top += btnH + gap
        menuButtonRect.set(cx - btnW / 2f, top, cx + btnW / 2f, top + btnH)
        Renderer.drawGlossyButton(canvas, menuButtonRect, "MENU", 0xFF607D8B.toInt(), 0xFFFFFFFF.toInt())
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

    // ── Dynamic Sky & Theme Rendering ──────────────────────────────

    private fun renderBackground(canvas: Canvas) {
        if (screenHeight != lastBgHeight || bgShader == null) {
            lastBgHeight = screenHeight
            val colors = when (currentTheme.id) {
                1 -> intArrayOf(0xFF2A0845.toInt(), 0xFFFF4E50.toInt(), 0xFFF9D423.toInt()) // Sunset
                2 -> intArrayOf(0xFF030811.toInt(), 0xFF0D1B2A.toInt(), 0xFF1B263B.toInt()) // Night
                3 -> intArrayOf(0xFF090014.toInt(), 0xFF240046.toInt(), 0xFF3C096C.toInt()) // Cosmic Space
                4 -> intArrayOf(0xFF00171F.toInt(), 0xFF003459.toInt(), 0xFF007EA7.toInt()) // Ocean
                5 -> intArrayOf(0xFF880E4F.toInt(), 0xFFE91E63.toInt(), 0xFFF8BBD0.toInt()) // Sakura
                else -> intArrayOf(0xFF004D40.toInt(), 0xFF00838F.toInt(), 0xFF26C6DA.toInt()) // Teal Sky
            }
            bgShader = LinearGradient(
                0f, 0f, 0f, screenHeight,
                colors, null, Shader.TileMode.CLAMP
            )
            bgGradientPaint.shader = bgShader
        }
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, bgGradientPaint)

        // Space / Night themes: draw starry background dust
        if (currentTheme.id == 3 || currentTheme.id == 2) {
            renderStarfield(canvas)
        }

        // Sunset theme: distant mountains silhouette
        if (currentTheme.id == 1) {
            renderSunsetMountains(canvas)
        }
    }

    private fun renderStarfield(canvas: Canvas) {
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
        val camX = entityManager.cameraX * 0.08f
        val camY = entityManager.cameraY * 0.08f
        for (i in 0 until 40) {
            val h = (i * 73856093L) and 0x7FFFFFFF
            val sx = ((h % screenWidth.toInt()) - camX) % screenWidth
            val sy = (((h / 100) % screenHeight.toInt()) - camY) % screenHeight
            val px = if (sx < 0) sx + screenWidth else sx
            val py = if (sy < 0) sy + screenHeight else sy
            starPaint.alpha = (90 + (h % 165)).toInt()
            val r = if (i % 6 == 0) 2.5f else 1.4f
            canvas.drawCircle(px, py, r, starPaint)
        }
    }

    private fun renderSunsetMountains(canvas: Canvas) {
        val mtnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x882A0845.toInt()
            style = Paint.Style.FILL
        }
        val path = android.graphics.Path()
        val baseY = screenHeight * 0.90f
        path.moveTo(0f, screenHeight)
        path.lineTo(0f, baseY)
        path.lineTo(screenWidth * 0.28f, baseY - screenHeight * 0.045f)
        path.lineTo(screenWidth * 0.62f, baseY + screenHeight * 0.02f)
        path.lineTo(screenWidth, baseY - screenHeight * 0.035f)
        path.lineTo(screenWidth, screenHeight)
        path.close()
        canvas.drawPath(path, mtnPaint)
    }

    // ── Scrolling clouds ─────────────────────────────────────────────

    private fun renderClouds(canvas: Canvas) {
        // Subtle clouds for non-space themes
        if (currentTheme.id == 3) return

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
        val pad = screenWidth * 0.035f
        val safeTop = GameSurfaceView.safeInsetTop
        val big = screenWidth * 0.058f
        val small = screenWidth * 0.028f
        val t = entityManager.survivalTime
        val totalSeconds = t.toInt()
        val timeStr = String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60)
        val cardH = big * 2.15f

        // ── Level Progress Bar (placed at the very top, strictly ABOVE score & star cards) ──
        val progH = small * 1.55f
        var progBottom = safeTop
        if (effectiveLevelConfig != null) {
            val cfg = effectiveLevelConfig
            val progW = screenWidth * 0.84f
            val progX = (screenWidth - progW) / 2f
            val progY = (safeTop + 4f).coerceAtLeast(6f)
            progBottom = progY + progH

            val pRect = RectF(progX, progY, progX + progW, progY + progH)
            Renderer.drawGlassPanel(canvas, pRect, 0xFF00E5FF.toInt(), progH / 2f, 0xEE0B1528.toInt(), 0xEE040814.toInt())

            val fillPillW = (progW - 6f) * entityManager.levelProgress
            if (fillPillW > 0f) {
                val fRect = RectF(progX + 3f, progY + 3f, progX + 3f + fillPillW, progY + progH - 3f)
                Renderer.drawProgressPill(canvas, fRect, entityManager.levelProgress, 0xFF00E676.toInt())
            }

            // Moving Jet icon on track
            val jetX = (progX + 3f + fillPillW).coerceIn(progX + 14f, progX + progW - 18f)
            Renderer.drawShadowText(canvas, "✈", jetX, progY + progH * 0.74f, small * 0.90f, 0xFFFFFFFF.toInt(), Paint.Align.CENTER)

            // Finish Carrier flag on the right edge
            Renderer.drawShadowText(canvas, "🏁", progX + progW - 10f, progY + progH * 0.74f, small * 0.75f, 0xFFFFD700.toInt(), Paint.Align.CENTER)

            // Dynamic progress readout: Distance completed / Target distance (percentage) + Remaining distance
            val curM = (cfg.distanceMeters * entityManager.levelProgress).toInt()
            val remainingM = (cfg.distanceMeters - curM).coerceAtLeast(0)
            val progPercent = (entityManager.levelProgress * 100).toInt()
            val progLabel = "LVL ${cfg.levelNumber}: $curM / ${cfg.distanceMeters}m ($progPercent%)  •  ${remainingM}m LEFT"
            Renderer.drawShadowText(canvas, progLabel, screenWidth / 2f, progY + progH * 0.72f, small * 0.70f, 0xFFFFD700.toInt(), Paint.Align.CENTER)
        }

        // ── Cards vertical position (strictly below progress bar, zero overlap) ──
        val topY = if (effectiveLevelConfig != null) {
            maxOf(safeTop + pad, progBottom + 8f)
        } else {
            safeTop + pad
        }

        // ── Score card (top-left) ───────────────────────────────────
        val leftW = screenWidth * 0.42f
        val leftRect = RectF(pad, topY, pad + leftW, topY + cardH)
        Renderer.drawGlassPanel(canvas, leftRect, 0xFF00E5FF.toInt())
        Renderer.drawShadowText(canvas, "SCORE", leftRect.left + 16f, topY + small * 1.25f, small * 0.8f, 0xFF7FDBFF.toInt())
        Renderer.drawShadowText(canvas, "${entityManager.score}", leftRect.left + 16f, topY + small * 1.25f + big * 0.95f, big, 0xFFFFFFFF.toInt())
        Renderer.drawShadowText(
            canvas, "\uD83C\uDFC6 ${GameData.highScore.coerceAtLeast(entityManager.score)}",
            leftRect.right - 14f, topY + small * 1.25f, small * 0.85f, 0xFFFFD700.toInt(), Paint.Align.RIGHT
        )

        // ── Stars / time card (top-right) ───────────────────────────
        val rightW = screenWidth * 0.36f
        val rightRect = RectF(screenWidth - pad - rightW, topY, screenWidth - pad, topY + cardH)
        Renderer.drawGlassPanel(canvas, rightRect, 0xFFFFD700.toInt())
        val starPulse = 1f + 0.08f * sin(t * 5f).toFloat()
        Renderer.drawStar(canvas, Vector2(rightRect.left + 30f, topY + cardH * 0.32f), small * 0.95f * starPulse, 5, Constants.STAR_COLOR)
        Renderer.drawShadowText(canvas, "${entityManager.starsCollected}", rightRect.left + 30f + small * 1.5f, topY + cardH * 0.32f + small * 0.55f, big * 0.8f, 0xFFFFFFFF.toInt())
        Renderer.drawShadowText(canvas, "\u23F1 $timeStr", rightRect.left + 18f, topY + cardH * 0.82f, small * 0.95f, 0xFF00E5FF.toInt())
        Renderer.drawShadowText(canvas, "+${entityManager.starsCollected}\uD83E\uDE99", rightRect.right - 14f, topY + cardH * 0.82f, small * 0.95f, 0xFFFFD700.toInt(), Paint.Align.RIGHT)

        // ── Pause button (round glass) ──────────────────────────────
        val ps = big * 1.3f
        val py = topY + cardH + 14f
        pauseButtonRect.set(pad, py, pad + ps, py + ps)
        Renderer.drawGlassPanel(canvas, pauseButtonRect, 0xFF00E5FF.toInt(), ps / 2f)
        pauseIconPaint.color = 0xFFFFFFFF.toInt()
        val bw = ps * 0.14f
        val bh = ps * 0.38f
        val bTop = py + (ps - bh) / 2f
        canvas.drawRoundRect(RectF(pad + ps * 0.34f - bw / 2f, bTop, pad + ps * 0.34f + bw / 2f, bTop + bh), 3f, 3f, pauseIconPaint)
        canvas.drawRoundRect(RectF(pad + ps * 0.66f - bw / 2f, bTop, pad + ps * 0.66f + bw / 2f, bTop + bh), 3f, 3f, pauseIconPaint)

        // ── Power-up chips (right column, with timer pills) ─────────
        val warnTime = Constants.POWERUP_WARN_TIME
        val p = entityManager.player
        val bars = listOf(
            BarInfo(p.shieldActive, Float.MAX_VALUE, Float.MAX_VALUE, "\uD83D\uDEE1 SHIELD", "SHIELD", Constants.SHIELD_COLOR),
            BarInfo(p.speedBoostActive, p.speedBoostTimer, Constants.SPEED_BOOST_DURATION, "\u26A1 BOOST", "BOOST ENDING!", Constants.SPEED_BOOST_COLOR),
            BarInfo(p.slowMotionActive, p.slowMotionTimer, Constants.SLOW_MOTION_DURATION, "\u231B SLOW-MO", "SLOW-MO ENDING!", Constants.SLOW_MOTION_COLOR),
            BarInfo(p.missileJammerActive, p.missileJammerTimer, Constants.MISSILE_JAMMER_DURATION, "\uD83D\uDCE1 JAMMER", "JAMMER ENDING!", Constants.MISSILE_JAMMER_COLOR),
            BarInfo(p.doubleScoreActive, p.doubleScoreTimer, Constants.DOUBLE_SCORE_DURATION, "2X SCORE", "2X ENDING!", Constants.DOUBLE_SCORE_COLOR),
            BarInfo(p.bulletShootActive, p.bulletShootTimer, Constants.BULLET_SHOOT_DURATION, "\uD83C\uDFAF BULLET", "BULLET ENDING!", Constants.BULLET_SHOOT_COLOR)
        )
        val chipW = rightW
        val chipH = small * 2.5f
        var chipY = topY + cardH + 14f
        for (bar in bars) {
            if (!bar.active) continue
            val expiring = bar.timer in 0f..warnTime && bar.duration != Float.MAX_VALUE
            val flash = !expiring || (t * 8f).toInt() % 2 == 0
            val chip = RectF(screenWidth - pad - chipW, chipY, screenWidth - pad, chipY + chipH)
            Renderer.drawGlassPanel(canvas, chip, bar.color.toInt(), chipH * 0.35f)
            Renderer.drawShadowText(
                canvas, if (expiring) bar.expiringLabel else bar.label,
                chip.left + 12f, chip.top + chipH * 0.46f, small * 0.8f,
                bar.color.toInt(), Paint.Align.LEFT, if (flash) 255 else 110
            )
            val fill = if (bar.duration == Float.MAX_VALUE) 1f else (bar.timer / bar.duration).coerceIn(0f, 1f)
            val pill = RectF(chip.left + 12f, chip.bottom - chipH * 0.27f, chip.right - 12f, chip.bottom - chipH * 0.12f)
            Renderer.drawProgressPill(canvas, pill, fill, bar.color.toInt())
            chipY += chipH + 8f
        }

        // ── Combo badge ─────────────────────────────────────────────
        if (entityManager.comboActive) {
            val pulse = 1f + 0.06f * sin(t * 6f).toFloat()
            val bw2 = screenWidth * 0.34f * pulse
            val bh2 = small * 2.2f * pulse
            val cy = screenHeight * 0.115f
            val badge = RectF(screenWidth / 2f - bw2 / 2f, cy - bh2 / 2f, screenWidth / 2f + bw2 / 2f, cy + bh2 / 2f)
            Renderer.drawGlassPanel(canvas, badge, 0xFFFF9100.toInt(), bh2 / 2f, 0xE6663300.toInt(), 0xE6331A00.toInt())
            Renderer.drawShadowText(canvas, "\uD83D\uDD25 COMBO x${entityManager.starStreak}", screenWidth / 2f, cy + small * 0.4f, small * 1.05f, 0xFFFFD54F.toInt(), Paint.Align.CENTER)
        }

        // ── Missile destroy streak (combo dots) ──────────────────────
        val streak = entityManager.missileDestroyStreak
        if (streak > 0) {
            val need = Constants.MISSILE_DESTROY_STREAK_FOR_SHIELD
            val sy = screenHeight * 0.265f
            val dotR = small * 0.38f
            val gap = dotR * 3f
            val startX = screenWidth / 2f - (need - 1) * gap / 2f
            Renderer.drawShadowText(canvas, "COMBO STREAK", screenWidth / 2f, sy - dotR * 2.2f, small * 0.7f, 0xFFFFD54F.toInt(), Paint.Align.CENTER)
            for (i in 0 until need) {
                val filled = i < streak
                Renderer.drawCircle(canvas, Vector2(startX + i * gap, sy), dotR, if (filled) 0xFFFFB300L else 0x55FFFFFFL)
            }
        }

        // ── Boss warning + HP in one panel ──────────────────────────
        if (entityManager.bossAlive) {
            val pulse = 0.65f + 0.35f * sin(t * 8f).toFloat()
            val warnW = screenWidth * 0.78f
            val warnH = big * 1.9f
            val warnY = screenHeight * 0.19f
            val warnRect = RectF(screenWidth / 2f - warnW / 2f, warnY - warnH / 2f, screenWidth / 2f + warnW / 2f, warnY + warnH / 2f)
            Renderer.drawGlassPanel(canvas, warnRect, 0xFFFF1744.toInt(), 16f, 0xEE4A0010.toInt(), 0xEE1A0008.toInt())
            Renderer.drawShadowText(
                canvas, "\u26A0 TITAN MISSILE INCOMING", screenWidth / 2f, warnRect.top + warnH * 0.4f,
                small * 0.95f, 0xFFFF5252.toInt(), Paint.Align.CENTER, (pulse * 255).toInt().coerceIn(0, 255)
            )
            val boss = entityManager.bosses.firstOrNull()
            if (boss != null && boss.alive) {
                val frac = (boss.hp.toFloat() / boss.maxHp.toFloat()).coerceIn(0f, 1f)
                val hp = RectF(warnRect.left + 16f, warnRect.bottom - warnH * 0.34f, warnRect.right - 16f, warnRect.bottom - warnH * 0.16f)
                Renderer.drawProgressPill(canvas, hp, frac, 0xFFFF1744.toInt())
            }
        }

        // ── Tutorial tooltip (first game only) ──────────────────────
        if (!GameData.tutorialShown && tutorialStep < tutorialMessages.size) {
            val msg = tutorialMessages[tutorialStep]
            val tipW = screenWidth * 0.84f
            val tipH = screenWidth * 0.11f
            val tipY = screenHeight * 0.88f
            val tip = RectF(screenWidth / 2f - tipW / 2f, tipY - tipH / 2f, screenWidth / 2f + tipW / 2f, tipY + tipH / 2f)
            val fadeIn = (tutorialTimer / 0.5f).coerceIn(0f, 1f)
            val fadeOut = if (tutorialTimer > 3.5f) 1f - ((tutorialTimer - 3.5f) / 0.5f).coerceIn(0f, 1f) else 1f
            val a = (fadeIn * fadeOut * 255).toInt().coerceIn(0, 255)
            canvas.saveLayerAlpha(tip.left - 10f, tip.top - 10f, tip.right + 10f, tip.bottom + 14f, a)
            Renderer.drawGlassPanel(canvas, tip, 0xFF00E5FF.toInt(), tipH / 2f)
            Renderer.drawShadowText(canvas, "\uD83D\uDCA1 $msg", screenWidth / 2f, tipY + screenWidth * 0.012f, screenWidth * 0.034f, 0xFFFFFFFF.toInt(), Paint.Align.CENTER)
            canvas.restore()
        }
    }

    // ── Pause overlay ─────────────────────────────────────────────────

    private fun renderPauseOverlay(canvas: Canvas) {
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, overlayPaint)
        val cx = screenWidth / 2f

        val cardW = screenWidth * 0.8f
        val cardH = screenHeight * 0.34f
        val card = RectF(cx - cardW / 2f, screenHeight * 0.33f, cx + cardW / 2f, screenHeight * 0.33f + cardH)
        Renderer.drawGlassPanel(canvas, card, 0xFF00E5FF.toInt(), 28f)

        Renderer.drawShadowText(canvas, "PAUSED", cx, card.top + cardH * 0.24f, screenWidth * 0.095f, 0xFFFFFFFF.toInt(), Paint.Align.CENTER)
        Renderer.drawShadowText(
            canvas, "Score ${entityManager.score}  \u2022  \u2605 ${entityManager.starsCollected}",
            cx, card.top + cardH * 0.38f, screenWidth * 0.036f, 0xFF7FDBFF.toInt(), Paint.Align.CENTER
        )

        val btnW = cardW * 0.8f
        val btnH = cardH * 0.18f
        val resumeTop = card.top + cardH * 0.50f
        pauseResumeRect.set(cx - btnW / 2f, resumeTop, cx + btnW / 2f, resumeTop + btnH)
        Renderer.drawGlossyButton(canvas, pauseResumeRect, "\u25B6  RESUME", 0xFF00E676.toInt(), 0xFF06210F.toInt())
        val menuTop = resumeTop + btnH + cardH * 0.1f
        pauseMenuRect.set(cx - btnW / 2f, menuTop, cx + btnW / 2f, menuTop + btnH)
        Renderer.drawGlossyButton(canvas, pauseMenuRect, "MENU", 0xFF607D8B.toInt(), 0xFFFFFFFF.toInt())
    }
}
