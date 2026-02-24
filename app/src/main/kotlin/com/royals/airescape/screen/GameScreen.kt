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
import com.royals.airescape.engine.Renderer
import com.royals.airescape.engine.SoundManager
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

    // ── Lottie blast ──────────────────────────────────────────────────
    private var blastLoaded = false

    // ── Helicopter sound ──
    private var helicopterActive = false

    // ── Sky / cloud colors ───────────────────────────────────────────
    private val skyColor = 0xFF80CBC4.toInt()  // nice teal sky

    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        alpha = 35
        style = Paint.Style.FILL
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

    private val shieldBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Constants.SHIELD_COLOR.toInt()
        style = Paint.Style.FILL
    }

    private val boostBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Constants.SPEED_BOOST_COLOR.toInt()
        style = Paint.Style.FILL
    }

    private val slowMotionBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Constants.SLOW_MOTION_COLOR.toInt()
        style = Paint.Style.FILL
    }

    private val jammerBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Constants.MISSILE_JAMMER_COLOR.toInt()
        style = Paint.Style.FILL
    }

    private val doubleScoreBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Constants.DOUBLE_SCORE_COLOR.toInt()
        style = Paint.Style.FILL
    }

    private val bulletShootBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Constants.BULLET_SHOOT_COLOR.toInt()
        style = Paint.Style.FILL
    }

    private val barRect = RectF()

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
    private var adRewardGiven = false
    private var interstitialTriggered = false

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
                }
                // Interstitial ad every 3rd game over
                if (!interstitialTriggered) {
                    interstitialTriggered = true
                    AdManager.gameOverCount++
                    if (AdManager.gameOverCount % 3 == 0) {
                        surfaceView.post { AdManager.showInterstitial() }
                    }
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

        // Camera transform for world-space entities
        canvas.save()
        canvas.translate(-entityManager.cameraX, -entityManager.cameraY)
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
                                GameData.totalStars += 20
                            }
                        )
                    }
                    return true
                }
                if (retryButtonRect.contains(x, y)) {
                    surfaceView.setScreen(GameScreen(surfaceView))
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

        // Load bullet bitmaps
        if (bulletBitmap == null) {
            bulletBitmap = loadBulletBitmap(R.drawable.bullet_red)
        }
        entityManager.bulletBitmap = bulletBitmap
        if (bulletGreenBitmap == null) {
            bulletGreenBitmap = loadBulletBitmap(R.drawable.bullet_green)
        }
        entityManager.bulletGreenBitmap = bulletGreenBitmap

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

        // Star icon + count
        goDetailPaint.textSize = screenWidth * 0.045f
        val starText = "\u2605  +${entityManager.starsCollected}"
        canvas.drawText(starText, cx, screenHeight * 0.44f, goDetailPaint)

        // Survival time
        val totalSec = entityManager.survivalTime.toInt()
        val min = totalSec / 60
        val sec = totalSec % 60
        val timeText = String.format("\u23F1  +%d:%02d", min, sec)
        canvas.drawText(timeText, cx, screenHeight * 0.49f, goDetailPaint)

        // Divider line
        val divW = screenWidth * 0.4f
        canvas.drawLine(cx - divW / 2f, screenHeight * 0.53f, cx + divW / 2f, screenHeight * 0.53f, goDividerPaint)

        // Total stars
        goDetailPaint.textSize = screenWidth * 0.04f
        canvas.drawText("\u2605  ${GameData.totalStars} total stars", cx, screenHeight * 0.575f, goDetailPaint)

        // New high score
        if (isNewHighScore) {
            val pulse = 0.7f + 0.3f * sin(gameOverAnimTimer * 4f).toFloat()
            goHighScorePaint.textSize = screenWidth * 0.055f
            goHighScorePaint.alpha = (pulse * 255).toInt().coerceIn(0, 255)
            canvas.drawText("NEW HIGH SCORE!", cx, screenHeight * 0.63f, goHighScorePaint)
        }

        // Layout buttons
        val btnW = screenWidth * 0.7f
        val btnH = screenHeight * 0.055f
        val gap = screenHeight * 0.018f

        val showWatchAd = AdManager.isRewardedReady() && !adRewardGiven
        var nextTop = screenHeight * 0.68f

        // Watch Ad button (gold) - only if rewarded ad available and not yet claimed
        if (showWatchAd) {
            watchAdButtonRect.set(cx - btnW / 2f, nextTop, cx + btnW / 2f, nextTop + btnH)
            Renderer.drawButton(canvas, watchAdButtonRect, "WATCH AD  +20 \u2605",
                color = 0xFFFFD600, textColor = 0xFF1A1A2E)
            nextTop += btnH + gap
        }

        // Retry button (green)
        retryButtonRect.set(cx - btnW / 2f, nextTop, cx + btnW / 2f, nextTop + btnH)
        Renderer.drawButton(canvas, retryButtonRect, "RETRY", color = 0xFF00E676, textColor = 0xFF1A1A2E)

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

        // Power-up timer bars (stacked below score)
        val barWidth = screenWidth * 0.3f
        val barHeight = screenHeight * 0.012f
        val barX = (screenWidth - barWidth) / 2f
        val barSpacing = screenHeight * 0.04f
        var barIndex = 0

        // Shield timer bar
        if (entityManager.player.shieldActive) {
            val barY = padding + hudTextSize * 1.8f + barIndex * barSpacing

            barRect.set(barX, barY, barX + barWidth, barY + barHeight)
            canvas.drawRoundRect(barRect, 4f, 4f, barBgPaint)

            val fill = 1f // shield has no timer — always full until missile hit
            barRect.set(barX, barY, barX + barWidth * fill, barY + barHeight)
            canvas.drawRoundRect(barRect, 4f, 4f, shieldBarPaint)

            Renderer.drawText(
                canvas, "SHIELD",
                screenWidth / 2f, barY - 4f,
                size = smallTextSize * 0.7f,
                color = Constants.SHIELD_COLOR,
                align = Paint.Align.CENTER
            )
            barIndex++
        }

        // Speed boost timer bar
        if (entityManager.player.speedBoostActive) {
            val barY = padding + hudTextSize * 1.8f + barIndex * barSpacing

            barRect.set(barX, barY, barX + barWidth, barY + barHeight)
            canvas.drawRoundRect(barRect, 4f, 4f, barBgPaint)

            val fill = (entityManager.player.speedBoostTimer / Constants.SPEED_BOOST_DURATION).coerceIn(0f, 1f)
            barRect.set(barX, barY, barX + barWidth * fill, barY + barHeight)
            canvas.drawRoundRect(barRect, 4f, 4f, boostBarPaint)

            Renderer.drawText(
                canvas, "BOOST",
                screenWidth / 2f, barY - 4f,
                size = smallTextSize * 0.7f,
                color = Constants.SPEED_BOOST_COLOR,
                align = Paint.Align.CENTER
            )
            barIndex++
        }

        // Slow motion timer bar
        if (entityManager.player.slowMotionActive) {
            val barY = padding + hudTextSize * 1.8f + barIndex * barSpacing

            barRect.set(barX, barY, barX + barWidth, barY + barHeight)
            canvas.drawRoundRect(barRect, 4f, 4f, barBgPaint)

            val fill = (entityManager.player.slowMotionTimer / Constants.SLOW_MOTION_DURATION).coerceIn(0f, 1f)
            barRect.set(barX, barY, barX + barWidth * fill, barY + barHeight)
            canvas.drawRoundRect(barRect, 4f, 4f, slowMotionBarPaint)

            Renderer.drawText(
                canvas, "SLOW-MO",
                screenWidth / 2f, barY - 4f,
                size = smallTextSize * 0.7f,
                color = Constants.SLOW_MOTION_COLOR,
                align = Paint.Align.CENTER
            )
            barIndex++
        }

        // Missile jammer timer bar
        if (entityManager.player.missileJammerActive) {
            val barY = padding + hudTextSize * 1.8f + barIndex * barSpacing

            barRect.set(barX, barY, barX + barWidth, barY + barHeight)
            canvas.drawRoundRect(barRect, 4f, 4f, barBgPaint)

            val fill = (entityManager.player.missileJammerTimer / Constants.MISSILE_JAMMER_DURATION).coerceIn(0f, 1f)
            barRect.set(barX, barY, barX + barWidth * fill, barY + barHeight)
            canvas.drawRoundRect(barRect, 4f, 4f, jammerBarPaint)

            Renderer.drawText(
                canvas, "JAMMER",
                screenWidth / 2f, barY - 4f,
                size = smallTextSize * 0.7f,
                color = Constants.MISSILE_JAMMER_COLOR,
                align = Paint.Align.CENTER
            )
            barIndex++
        }

        // Double score timer bar
        if (entityManager.player.doubleScoreActive) {
            val barY = padding + hudTextSize * 1.8f + barIndex * barSpacing

            barRect.set(barX, barY, barX + barWidth, barY + barHeight)
            canvas.drawRoundRect(barRect, 4f, 4f, barBgPaint)

            val fill = (entityManager.player.doubleScoreTimer / Constants.DOUBLE_SCORE_DURATION).coerceIn(0f, 1f)
            barRect.set(barX, barY, barX + barWidth * fill, barY + barHeight)
            canvas.drawRoundRect(barRect, 4f, 4f, doubleScoreBarPaint)

            Renderer.drawText(
                canvas, "2X SCORE",
                screenWidth / 2f, barY - 4f,
                size = smallTextSize * 0.7f,
                color = Constants.DOUBLE_SCORE_COLOR,
                align = Paint.Align.CENTER
            )
            barIndex++
        }

        // Bullet shoot timer bar
        if (entityManager.player.bulletShootActive) {
            val barY = padding + hudTextSize * 1.8f + barIndex * barSpacing

            barRect.set(barX, barY, barX + barWidth, barY + barHeight)
            canvas.drawRoundRect(barRect, 4f, 4f, barBgPaint)

            val fill = (entityManager.player.bulletShootTimer / Constants.BULLET_SHOOT_DURATION).coerceIn(0f, 1f)
            barRect.set(barX, barY, barX + barWidth * fill, barY + barHeight)
            canvas.drawRoundRect(barRect, 4f, 4f, bulletShootBarPaint)

            Renderer.drawText(
                canvas, "BULLET",
                screenWidth / 2f, barY - 4f,
                size = smallTextSize * 0.7f,
                color = Constants.BULLET_SHOOT_COLOR,
                align = Paint.Align.CENTER
            )
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
