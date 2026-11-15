package com.royals.airescape.screen

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.royals.airescape.ads.AdManager
import com.royals.airescape.data.Constants
import com.royals.airescape.data.GameData
import com.royals.airescape.engine.GameSurfaceView
import com.royals.airescape.engine.Renderer
import com.royals.airescape.engine.SoundManager
import com.royals.airescape.level.LevelConfig
import com.royals.airescape.level.LevelManager
import com.royals.airescape.util.Vector2
import kotlin.math.sin

/**
 * Celebratory Victory screen displayed when player reaches the Finish Line.
 * Awards 1 to 3 stars based on completion, star collection, and damage taken.
 */
class LevelCompleteScreen(
    private val surfaceView: GameSurfaceView,
    private val levelConfig: LevelConfig,
    private val starsCollected: Int,
    private val score: Int,
    private val flawless: Boolean,
    private val survivalTime: Float
) : Screen {

    private var screenWidth = 0f
    private var screenHeight = 0f
    private var animTimer = 0f

    // Calculated stars (1, 2, or 3)
    private val starsEarned: Int

    // Rectangles for interactive buttons
    private val nextLevelButtonRect = RectF()
    private val replayButtonRect = RectF()
    private val mapButtonRect = RectF()

    private val overlayPaint = Paint().apply {
        color = 0xDD070B14.toInt()
        style = Paint.Style.FILL
    }

    init {
        // Star calculation:
        // 1 Star: Reached the finish line! (Guaranteed)
        // 2 Stars: Collected at least 50% of level target stars
        // 3 Stars: Flawless flight (no damage taken) OR collected 100% of target stars
        var s = 1
        if (starsCollected >= (levelConfig.targetStars * 0.5f).toInt()) s++
        if (flawless || starsCollected >= levelConfig.targetStars) s++
        starsEarned = s.coerceIn(1, 3)

        // Save progress & award coins
        GameData.setLevelStars(levelConfig.levelNumber, starsEarned)
        GameData.setLevelHighScore(levelConfig.levelNumber, score)
        GameData.highestLevelUnlocked = (levelConfig.levelNumber + 1).coerceAtMost(LevelManager.TOTAL_LEVELS)
        GameData.totalStars += starsCollected
        GameData.addCoins(levelConfig.rewardCoins + starsCollected * Constants.COINS_PER_STAR)

        SoundManager.playPowerUp()
    }

    override fun update(dt: Float) {
        animTimer += dt
    }

    override fun render(canvas: Canvas) {
        if (screenWidth <= 0f || screenHeight <= 0f) {
            val sw = GameSurfaceView.screenWidth.toFloat()
            val sh = GameSurfaceView.screenHeight.toFloat()
            if (sw > 0f && sh > 0f) {
                screenWidth = sw
                screenHeight = sh
            } else {
                screenWidth = canvas.width.toFloat()
                screenHeight = canvas.height.toFloat()
            }
        }

        // Dark victory backdrop
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, overlayPaint)

        val cx = screenWidth / 2f
        val cardW = screenWidth * 0.88f
        val cardH = screenHeight * 0.72f
        val cardTop = screenHeight * 0.12f
        val card = RectF(cx - cardW / 2f, cardTop, cx + cardW / 2f, cardTop + cardH)

        // Glowing Victory Card
        Renderer.drawGlassPanel(canvas, card, 0xFF00E676.toInt(), 32f, 0xF00A192F.toInt(), 0xF0020C1B.toInt())

        val ch = card.height()

        // 1. Victory Header
        Renderer.drawShadowText(
            canvas, "MISSION ACCOMPLISHED!",
            cx, card.top + ch * 0.08f,
            screenWidth * 0.062f, 0xFF00E676.toInt(), Paint.Align.CENTER
        )
        Renderer.drawShadowText(
            canvas, levelConfig.title.uppercase(),
            cx, card.top + ch * 0.13f,
            screenWidth * 0.035f, 0xFF81D4FA.toInt(), Paint.Align.CENTER
        )

        // 2. Big 3-Star Celebration
        val starY = card.top + ch * 0.23f
        val starGap = screenWidth * 0.22f
        val starRadius = screenWidth * 0.08f

        for (i in 1..3) {
            val sx = cx + (i - 2) * starGap
            val starEarned = i <= starsEarned
            val delay = i * 0.25f
            val starAnim = ((animTimer - delay) / 0.3f).coerceIn(0f, 1f)
            val scale = if (starEarned) 1f + 0.15f * sin((animTimer + i) * 6f).toFloat() else 0.8f
            val curR = starRadius * scale * (if (starEarned) starAnim else 1f)

            // Star shape
            val color = if (starEarned) Constants.STAR_COLOR else 0x44FFFFFF
            Renderer.drawStar(canvas, Vector2(sx, starY), curR, 5, color)
        }

        // Subtitle below stars
        val starDesc = when (starsEarned) {
            3 -> "★ ★ ★  PERFECT 3-STAR VICTORY!  ★ ★ ★"
            2 -> "★ ★ ☆  GREAT FLIGHT!  ☆ ★ ★"
            else -> "★ ☆ ☆  MISSION SURVIVED!  ☆ ☆ ★"
        }
        Renderer.drawShadowText(
            canvas, starDesc, cx, card.top + ch * 0.32f,
            screenWidth * 0.034f, 0xFFFFD700.toInt(), Paint.Align.CENTER
        )

        // 3. Stat Tiles: Distance Flown, Stars Collected, Coins Earned
        val stats = listOf(
            Triple("🚩 DISTANCE", "${levelConfig.distanceMeters}m", "100% REACHED"),
            Triple("★ STARS", "+$starsCollected", "${(starsCollected.toFloat() / levelConfig.targetStars * 100).toInt()}%"),
            Triple("🪙 REWARD", "+${levelConfig.rewardCoins}", "VICTORY BONUS")
        )
        val tileGap = screenWidth * 0.02f
        val tileW = (cardW - tileGap * 4f) / 3f
        val tileH = ch * 0.17f
        val tileTop = card.top + ch * 0.37f

        stats.forEachIndexed { i, (label, value, sub) ->
            val l = card.left + tileGap + i * (tileW + tileGap)
            val tile = RectF(l, tileTop, l + tileW, tileTop + tileH)
            Renderer.drawGlassPanel(canvas, tile, 0x4400E5FF, 16f, 0x55112240, 0x550A192F)
            Renderer.drawShadowText(canvas, label, tile.centerX(), tile.top + tileH * 0.32f, screenWidth * 0.025f, 0xFF81D4FA.toInt(), Paint.Align.CENTER)
            Renderer.drawShadowText(canvas, value, tile.centerX(), tile.top + tileH * 0.64f, screenWidth * 0.046f, 0xFFFFFFFF.toInt(), Paint.Align.CENTER)
            Renderer.drawShadowText(canvas, sub, tile.centerX(), tile.bottom - tileH * 0.12f, screenWidth * 0.022f, 0xFF00E676.toInt(), Paint.Align.CENTER)
        }

        // Score summary
        Renderer.drawShadowText(
            canvas, "TOTAL SCORE: $score",
            cx, card.top + ch * 0.60f,
            screenWidth * 0.045f, 0xFFFFFFFF.toInt(), Paint.Align.CENTER
        )

        // 4. Action Buttons
        val btnW = cardW * 0.85f
        val btnH = screenHeight * 0.06f
        val gap = screenHeight * 0.015f
        var top = card.top + ch * 0.67f

        // Next Level Button (Prominent Glowing Green)
        nextLevelButtonRect.set(cx - btnW / 2f, top, cx + btnW / 2f, top + btnH)
        val hasNextLevel = levelConfig.levelNumber < LevelManager.TOTAL_LEVELS
        val nextLevelNumber = levelConfig.levelNumber + 1
        val nextBtnText = if (hasNextLevel) "NEXT LEVEL $nextLevelNumber  ▶" else "VICTORY MAP 🗺️"
        Renderer.drawGlossyButton(canvas, nextLevelButtonRect, nextBtnText, 0xFF00E676.toInt(), 0xFF052110.toInt())

        top += btnH + gap

        // Replay Button (Cyan)
        val halfW = (btnW - gap) / 2f
        replayButtonRect.set(cx - btnW / 2f, top, cx - gap / 2f, top + btnH * 0.9f)
        Renderer.drawGlossyButton(canvas, replayButtonRect, "REPLAY ↺", 0xFF00B0FF.toInt(), 0xFF021B2F.toInt())

        // Levels Map Button (Gold)
        mapButtonRect.set(cx + gap / 2f, top, cx + btnW / 2f, top + btnH * 0.9f)
        Renderer.drawGlossyButton(canvas, mapButtonRect, "MAP 🗺️", 0xFFFFB300.toInt(), 0xFF261800.toInt())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y

            if (nextLevelButtonRect.contains(x, y)) {
                if (levelConfig.levelNumber < LevelManager.TOTAL_LEVELS) {
                    val nextLvl = levelConfig.levelNumber + 1
                    val nextConfig = LevelManager.getLevel(nextLvl)
                    surfaceView.setScreen(GameScreen(surfaceView, levelConfig = nextConfig))
                } else {
                    surfaceView.setScreen(LevelSelectScreen(surfaceView))
                }
                return true
            }

            if (replayButtonRect.contains(x, y)) {
                surfaceView.setScreen(GameScreen(surfaceView, levelConfig = levelConfig))
                return true
            }

            if (mapButtonRect.contains(x, y)) {
                surfaceView.setScreen(LevelSelectScreen(surfaceView))
                return true
            }
        }
        return true
    }

    override fun onSizeChanged(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
    }

    override fun onBackPressed(): Boolean {
        surfaceView.setScreen(LevelSelectScreen(surfaceView))
        return true
    }
}
