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

        val btnW = sw * 0.7f
        val btnH = sh * 0.055f
        val centerX = sw / 2f
        val gap = sh * 0.018f

        val retryTop = sh * 0.68f
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

        // "GAME OVER" text
        val goSize = screenWidth * 0.12f
        gameOverPaint.textSize = goSize
        canvas.drawText("GAME OVER", centerX, screenHeight * 0.2f, gameOverPaint)

        // Score
        scorePaint.textSize = screenWidth * 0.09f
        canvas.drawText("$score", centerX, screenHeight * 0.33f, scorePaint)

        scorePaint.textSize = screenWidth * 0.035f
        canvas.drawText("SCORE", centerX, screenHeight * 0.35f, scorePaint)

        // Stars collected
        val starIcon = "\u2605" // filled star unicode
        detailPaint.textSize = screenWidth * 0.045f
        canvas.drawText(
            "$starIcon $starsCollected Stars Collected",
            centerX, screenHeight * 0.43f, detailPaint
        )

        // Survival time
        val totalSeconds = survivalTime.toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val timeStr = String.format("Survived %d:%02d", minutes, seconds)
        detailPaint.textSize = screenWidth * 0.04f
        canvas.drawText(timeStr, centerX, screenHeight * 0.49f, detailPaint)

        // New high score indicator (pulsing gold)
        if (isNewHighScore) {
            val pulse = 0.7f + 0.3f * sin(animTimer * 4f)
            newHighScorePaint.textSize = screenWidth * 0.06f
            newHighScorePaint.alpha = (pulse * 255).toInt().coerceIn(0, 255)
            canvas.drawText("NEW HIGH SCORE!", centerX, screenHeight * 0.59f, newHighScorePaint)
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
