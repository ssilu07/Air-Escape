package com.royals.airescape.screen

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.royals.airescape.data.Constants
import com.royals.airescape.data.GameData
import com.royals.airescape.engine.GameSurfaceView
import com.royals.airescape.engine.Renderer
import kotlin.math.sin

/**
 * Game-over screen shown after the player is hit.
 *
 * Displays the final score, stars collected, survival time,
 * and whether a new high score was achieved. Provides buttons
 * to retry or return to the main menu.
 */
class GameOverScreen(
    private val surfaceView: GameSurfaceView,
    private val score: Int,
    private val starsCollected: Int,
    private val survivalTime: Float
) : Screen {

    // ── Computed state ───────────────────────────────────────────────
    private var isNewHighScore = false
    private var scoreSubmitted = false

    // ── Layout ───────────────────────────────────────────────────────
    private var screenWidth = 0f
    private var screenHeight = 0f
    private var layoutDone = false

    // ── Buttons ──────────────────────────────────────────────────────
    private val retryButtonRect = RectF()
    private val menuButtonRect = RectF()

    // ── Animation ────────────────────────────────────────────────────
    private var animTimer = 0f

    // ── Paints ───────────────────────────────────────────────────────
    private val overlayPaint = Paint().apply {
        color = 0xFF000000.toInt()
        alpha = 180
        style = Paint.Style.FILL
    }

    private val gameOverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF1744.toInt()
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFCCCCCC.toInt()
        textAlign = Paint.Align.CENTER
    }

    private val newHighScorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Constants.STAR_COLOR.toInt()
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    // ── Initialisation ───────────────────────────────────────────────

    override fun onEnter() {
        submitScoreIfNeeded()
    }

    // ── Layout ───────────────────────────────────────────────────────

    private fun layoutIfNeeded() {
        val sw = GameSurfaceView.screenWidth.toFloat()
        val sh = GameSurfaceView.screenHeight.toFloat()
        if (sw <= 0f || sh <= 0f) return

        screenWidth = sw
        screenHeight = sh

        val safeTop = GameSurfaceView.safeInsetTop
        val safeBottom = GameSurfaceView.safeInsetBottom + GameSurfaceView.bannerHeight
        val usableH = (sh - safeTop - safeBottom).coerceAtLeast(sh * 0.70f)

        val btnW = sw * 0.7f
        val btnH = usableH * 0.065f
        val centerX = sw / 2f
        val gap = usableH * 0.020f

        val retryTop = safeTop + usableH * 0.68f
        retryButtonRect.set(
            centerX - btnW / 2f, retryTop,
            centerX + btnW / 2f, retryTop + btnH
        )

        val menuTop = retryTop + btnH + gap
        menuButtonRect.set(
            centerX - btnW / 2f, menuTop,
            centerX + btnW / 2f, menuTop + btnH
        )

        layoutDone = true

        submitScoreIfNeeded()
    }

    private fun submitScoreIfNeeded() {
        if (scoreSubmitted) return
        scoreSubmitted = true
        isNewHighScore = score > GameData.highScore
        GameData.submitScore(score, starsCollected)
    }

    // ── Screen interface ─────────────────────────────────────────────

    override fun update(dt: Float) {
        if (!layoutDone) layoutIfNeeded()
        animTimer += dt
    }

    override fun render(canvas: Canvas) {
        if (!layoutDone) return

        // Semi-transparent dark overlay
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, overlayPaint)

        val centerX = screenWidth / 2f
        val safeTop = GameSurfaceView.safeInsetTop
        val safeBottom = GameSurfaceView.safeInsetBottom + GameSurfaceView.bannerHeight
        val usableH = (screenHeight - safeTop - safeBottom).coerceAtLeast(screenHeight * 0.70f)

        // "GAME OVER" text with neon glow
        val goSize = screenWidth * 0.11f
        gameOverPaint.textSize = goSize
        canvas.drawText("GAME OVER", centerX, safeTop + usableH * 0.16f, gameOverPaint)

        // Rank determination based on score
        val (rankTitle, rankColor) = when {
            score >= 3000 -> "TITAN SLAYER [DIAMOND]" to 0xFF00E5FF.toInt()
            score >= 1500 -> "ELITE COMMANDER [GOLD]" to 0xFFFFD700.toInt()
            score >= 600 -> "VETERAN ACE [SILVER]" to 0xFFE0E0E0.toInt()
            else -> "ROOKIE CADET [BRONZE]" to 0xFFCD7F32.toInt()
        }

        // Debrief Tech Card (Screenshot 5 style)
        val cardW = screenWidth * 0.86f
        val cardTop = safeTop + usableH * 0.20f
        val cardBottom = safeTop + usableH * 0.65f
        val cardRect = RectF(centerX - cardW / 2f, cardTop, centerX + cardW / 2f, cardBottom)
        Renderer.drawTechPanel(canvas, cardRect, 0xEE0D1B2A.toInt(), rankColor, 16f)

        // Pilot Rank Badge
        detailPaint.textSize = screenWidth * 0.038f
        detailPaint.color = rankColor
        detailPaint.isFakeBoldText = true
        canvas.drawText("\uD83C\uDFC6 PILOT RANK: $rankTitle", centerX, cardTop + screenWidth * 0.08f, detailPaint)
        detailPaint.isFakeBoldText = false

        // Score
        scorePaint.textSize = screenWidth * 0.10f
        canvas.drawText("$score", centerX, cardTop + screenWidth * 0.20f, scorePaint)

        scorePaint.textSize = screenWidth * 0.032f
        scorePaint.color = 0xFF81D4FA.toInt()
        canvas.drawText("FINAL SCORE  \u2022  HIGH: ${GameData.highScore.coerceAtLeast(score)}", centerX, cardTop + screenWidth * 0.25f, scorePaint)
        scorePaint.color = 0xFFFFFFFF.toInt()

        // Stars + Coins collected (1 Star = 1 Coin)
        val starIcon = "\u2605"
        val coinIcon = "\uD83E\uDE99"
        detailPaint.textSize = screenWidth * 0.040f
        detailPaint.color = 0xFFFFD700.toInt()
        canvas.drawText(
            "$starIcon $starsCollected Stars  |  $coinIcon $starsCollected Coins",
            centerX, cardTop + screenWidth * 0.33f, detailPaint
        )

        // Wallet balance
        detailPaint.textSize = screenWidth * 0.036f
        detailPaint.color = 0xFFCCCCCC.toInt()
        val walletRs = String.format(java.util.Locale.US, "%.2f", GameData.totalCoins / Constants.COINS_PER_RUPEE.toFloat())
        canvas.drawText(
            "Wallet Balance: $coinIcon ${GameData.totalCoins} Coins (\u20B9$walletRs)",
            centerX, cardTop + screenWidth * 0.40f, detailPaint
        )

        // Survival time
        val totalSeconds = survivalTime.toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val timeStr = String.format("Survived %d:%02d", minutes, seconds)
        detailPaint.textSize = screenWidth * 0.038f
        detailPaint.color = 0xFF00E5FF.toInt()
        canvas.drawText("\u23F1 $timeStr", centerX, cardTop + screenWidth * 0.47f, detailPaint)

        // New high score indicator (pulsing gold)
        if (isNewHighScore) {
            val pulse = 0.7f + 0.3f * sin(animTimer * 4f)
            newHighScorePaint.textSize = screenWidth * 0.055f
            newHighScorePaint.alpha = (pulse * 255).toInt().coerceIn(0, 255)
            canvas.drawText("\u26A0 NEW HIGH SCORE! \u26A0", centerX, cardTop + screenWidth * 0.55f, newHighScorePaint)
        }

        // Retry button (green)
        Renderer.drawButton(
            canvas, retryButtonRect, "RETRY",
            color = 0xFF00E676,
            textColor = 0xFF1A1A2E
        )

        // Menu button (gray)
        Renderer.drawButton(
            canvas, menuButtonRect, "MENU",
            color = 0xFF78909C,
            textColor = 0xFFFFFFFF
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && layoutDone) {
            val x = event.x
            val y = event.y

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
}
