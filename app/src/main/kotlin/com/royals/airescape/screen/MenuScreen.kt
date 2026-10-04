package com.royals.airescape.screen

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.royals.airescape.ads.AdManager
import com.royals.airescape.data.Constants
import com.royals.airescape.data.GameData
import com.royals.airescape.engine.GameSurfaceView
import com.royals.airescape.engine.PlayGamesManager
import com.royals.airescape.engine.Renderer
import com.royals.airescape.util.Vector2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Main menu screen.
 * Shows the game title, buttons for Play / Planes / Settings,
 * the current high score, and animated background missiles.
 */
class MenuScreen(private val surfaceView: GameSurfaceView) : Screen {

    // ── Button regions ───────────────────────────────────────────────
    private val playButtonRect = RectF()
    private val normalModeRect = RectF()
    private val hardModeRect = RectF()
    private val planesButtonRect = RectF()
    private val themesButtonRect = RectF()
    private val missileGuideButtonRect = RectF()
    private val leaderboardButtonRect = RectF()
    private val walletButtonRect = RectF()
    private val settingsButtonRect = RectF()


    // ── Layout state ─────────────────────────────────────────────────
    private var screenWidth = 0f
    private var screenHeight = 0f
    private var layoutDone = false

    // ── Background missiles ──────────────────────────────────────────
    private data class BgMissile(
        var pos: Vector2,
        var vel: Vector2,
        var angle: Float,
        var alpha: Int
    )

    private val bgMissiles = mutableListOf<BgMissile>()
    private var bgSpawnTimer = 0f

    // ── Title animation ──────────────────────────────────────────────
    private var titleTimer = 0f

    // ── Paints ───────────────────────────────────────────────────────
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
        color = 0xFFFFFFFF.toInt()
    }

    private val titleGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
        color = 0xFFFF1744.toInt()
        alpha = 60
    }

    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = 0xFFBBBBBB.toInt()
        textSize = 28f
    }

    private val missilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF555555.toInt()
    }

    private val modeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val modeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val modeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    // ── Layout ───────────────────────────────────────────────────────

    private fun layoutIfNeeded() {
        val sw = GameSurfaceView.screenWidth.toFloat()
        val sh = GameSurfaceView.screenHeight.toFloat()
        if (sw <= 0f || sh <= 0f) return

        screenWidth = sw
        screenHeight = sh

        val btnW = sw * 0.7f
        val btnH = sh * 0.048f
        val centerX = sw / 2f
        val gap = sh * 0.012f
        val padding = sw * 0.06f
        val halfBtnW = (btnW - gap) / 2f

        // Mode buttons (NORMAL / HARD) - horizontal row
        val modeTop = sh * 0.30f
        val modeH = btnH * 0.85f
        val modeLeft = centerX - btnW / 2f
        normalModeRect.set(modeLeft, modeTop, modeLeft + halfBtnW, modeTop + modeH)
        hardModeRect.set(modeLeft + halfBtnW + gap, modeTop, modeLeft + halfBtnW * 2f + gap, modeTop + modeH)

        // Play button — full width
        val playTop = modeTop + modeH + gap * 1.5f
        playButtonRect.set(centerX - btnW / 2f, playTop, centerX + btnW / 2f, playTop + btnH)

        // Row: PLANES | THEMES — side by side
        val row2Top = playTop + btnH + gap
        planesButtonRect.set(modeLeft, row2Top, modeLeft + halfBtnW, row2Top + btnH)
        themesButtonRect.set(modeLeft + halfBtnW + gap, row2Top, modeLeft + halfBtnW * 2f + gap, row2Top + btnH)

        // Row: MISSILE GUIDE | LEADERBOARD — side by side
        val row3Top = row2Top + btnH + gap
        missileGuideButtonRect.set(modeLeft, row3Top, modeLeft + halfBtnW, row3Top + btnH)
        leaderboardButtonRect.set(modeLeft + halfBtnW + gap, row3Top, modeLeft + halfBtnW * 2f + gap, row3Top + btnH)

        // Row: WALLET | SETTINGS — side by side
        val row4Top = row3Top + btnH + gap
        walletButtonRect.set(modeLeft, row4Top, modeLeft + halfBtnW, row4Top + btnH)
        settingsButtonRect.set(modeLeft + halfBtnW + gap, row4Top, modeLeft + halfBtnW * 2f + gap, row4Top + btnH)

        layoutDone = true


        // Seed some initial background missiles
        for (i in 0..5) {
            spawnBgMissile()
        }
    }

    // ── Screen interface ─────────────────────────────────────────────

    override fun onEnter() {
        AdManager.showBanner()
    }

    override fun onExit() {
        AdManager.hideBanner()
    }

    override fun update(dt: Float) {
        if (!layoutDone) layoutIfNeeded()
        if (!layoutDone) return

        titleTimer += dt

        // Background missile animation
        bgSpawnTimer += dt
        if (bgSpawnTimer >= 2.0f) {
            bgSpawnTimer = 0f
            spawnBgMissile()
        }

        val iter = bgMissiles.iterator()
        while (iter.hasNext()) {
            val m = iter.next()
            m.pos = m.pos + m.vel * dt
            // Remove if off screen
            if (m.pos.x < -60f || m.pos.x > screenWidth + 60f ||
                m.pos.y < -60f || m.pos.y > screenHeight + 60f
            ) {
                iter.remove()
            }
        }
    }

    override fun render(canvas: Canvas) {
        if (!layoutDone) return

        // Background color (dark)
        canvas.drawColor(Constants.BACKGROUND_COLOR.toInt())

        // Background missiles
        renderBackgroundMissiles(canvas)

        // Title with subtle glow
        val titleSize = screenWidth * 0.12f
        val titleY = screenHeight * 0.14f
        val glowOffset = sin(titleTimer * 2f) * 2f

        titleGlowPaint.textSize = titleSize + 4f
        canvas.drawText("MISSILES!", screenWidth / 2f, titleY + glowOffset, titleGlowPaint)

        titlePaint.textSize = titleSize
        canvas.drawText("MISSILES!", screenWidth / 2f, titleY, titlePaint)

        // Subtitle
        subtitlePaint.textSize = screenWidth * 0.032f
        canvas.drawText("Dodge. Survive. Collect.", screenWidth / 2f, titleY + titleSize * 0.45f, subtitlePaint)

        // Mode buttons (NORMAL / HARD)
        val isHard = GameData.hardMode
        drawModeButton(canvas, normalModeRect, "NORMAL", !isHard)
        drawModeButton(canvas, hardModeRect, "HARD", isHard)

        // Play button (green)
        Renderer.drawButton(
            canvas, playButtonRect, "PLAY",
            color = 0xFF00E676,
            textColor = 0xFF1A1A2E
        )

        // Planes button (blue)
        Renderer.drawButton(
            canvas, planesButtonRect, "PLANES",
            color = 0xFF40C4FF,
            textColor = 0xFF1A1A2E
        )

        // Themes button (purple)
        Renderer.drawButton(
            canvas, themesButtonRect, "THEMES",
            color = 0xFF7C4DFF,
            textColor = 0xFFFFFFFF
        )

        // Missile guide button (red)
        Renderer.drawButton(
            canvas, missileGuideButtonRect, "MISSILE GUIDE",
            color = 0xFFFF1744,
            textColor = 0xFFFFFFFF
        )

        // Leaderboard button (gold)
        Renderer.drawButton(
            canvas, leaderboardButtonRect, "LEADERBOARD",
            color = 0xFFFFD740,
            textColor = 0xFF1A1A2E
        )

        // Wallet button (gold)
        Renderer.drawButton(
            canvas, walletButtonRect, "\uD83E\uDE99 WALLET",
            color = 0xFFFFD700,
            textColor = 0xFF1A1A2E
        )

        // Settings button (gray)
        Renderer.drawButton(
            canvas, settingsButtonRect, "SETTINGS",
            color = 0xFF78909C,
            textColor = 0xFFFFFFFF
        )

        // High score + stars + coins (below settings, spaced nicely)
        val infoY = walletButtonRect.bottom + screenHeight * 0.035f
        Renderer.drawText(
            canvas, "HIGH SCORE: ${GameData.highScore}",
            screenWidth / 2f, infoY,
            size = screenWidth * 0.038f,
            color = Constants.STAR_COLOR,
            align = Paint.Align.CENTER
        )
        val walletRs = String.format(java.util.Locale.US, "%.2f", GameData.totalCoins / Constants.COINS_PER_RUPEE.toFloat())
        Renderer.drawText(
            canvas, "\u2605 ${GameData.totalStars} Stars   \u2022   \uD83E\uDE99 ${GameData.totalCoins} Coins (\u20B9$walletRs)",
            screenWidth / 2f, infoY + screenWidth * 0.05f,
            size = screenWidth * 0.032f,
            color = 0xFFE0E0E0,
            align = Paint.Align.CENTER
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && layoutDone) {
            val x = event.x
            val y = event.y

            if (normalModeRect.contains(x, y)) {
                GameData.hardMode = false
                return true
            }

            if (hardModeRect.contains(x, y)) {
                GameData.hardMode = true
                return true
            }

            if (playButtonRect.contains(x, y)) {
                surfaceView.setScreen(GameScreen(surfaceView))
                return true
            }

            if (planesButtonRect.contains(x, y)) {
                surfaceView.setScreen(PlaneSelectScreen(surfaceView))
                return true
            }

            if (themesButtonRect.contains(x, y)) {
                surfaceView.setScreen(ThemeSelectScreen(surfaceView))
                return true
            }

            if (missileGuideButtonRect.contains(x, y)) {
                surfaceView.setScreen(MissileInfoScreen(surfaceView))
                return true
            }

            if (leaderboardButtonRect.contains(x, y)) {
                PlayGamesManager.showLeaderboard()
                return true
            }

            if (walletButtonRect.contains(x, y)) {
                surfaceView.setScreen(WalletScreen(surfaceView))
                return true
            }

            if (settingsButtonRect.contains(x, y)) {
                surfaceView.setScreen(SettingsScreen(surfaceView))
                return true
            }
        }
        return true
    }


    // ── Background missiles ──────────────────────────────────────────

    private fun spawnBgMissile() {
        if (screenWidth <= 0f) return

        val edge = Random.nextInt(4)
        val pos = when (edge) {
            0 -> Vector2(Random.nextFloat() * screenWidth, -30f)
            1 -> Vector2(Random.nextFloat() * screenWidth, screenHeight + 30f)
            2 -> Vector2(-30f, Random.nextFloat() * screenHeight)
            else -> Vector2(screenWidth + 30f, Random.nextFloat() * screenHeight)
        }

        // Aim generally toward center with some randomness
        val target = Vector2(
            screenWidth * (0.2f + Random.nextFloat() * 0.6f),
            screenHeight * (0.2f + Random.nextFloat() * 0.6f)
        )
        val dir = (target - pos).normalized()
        val speed = 40f + Random.nextFloat() * 60f
        val vel = dir * speed
        val angle = dir.angle()

        bgMissiles.add(
            BgMissile(
                pos = pos,
                vel = vel,
                angle = angle,
                alpha = 30 + Random.nextInt(40)
            )
        )
    }

    private fun drawModeButton(canvas: Canvas, rect: RectF, label: String, selected: Boolean) {
        if (selected) {
            modeBgPaint.color = 0xFF00E676.toInt()
            modeBgPaint.alpha = 50
            modeBorderPaint.color = 0xFF00E676.toInt()
        } else {
            modeBgPaint.color = 0xFF333333.toInt()
            modeBgPaint.alpha = 180
            modeBorderPaint.color = 0xFF555555.toInt()
        }
        canvas.drawRoundRect(rect, 14f, 14f, modeBgPaint)
        canvas.drawRoundRect(rect, 14f, 14f, modeBorderPaint)

        modeTextPaint.textSize = rect.height() * 0.42f
        modeTextPaint.color = if (selected) 0xFF00E676.toInt() else 0xFFAAAAAA.toInt()
        val textY = rect.centerY() - (modeTextPaint.descent() + modeTextPaint.ascent()) / 2f
        canvas.drawText(label, rect.centerX(), textY, modeTextPaint)
    }

    private fun renderBackgroundMissiles(canvas: Canvas) {
        for (m in bgMissiles) {
            // Trail
            val trailLen = 20f
            val trailEnd = m.pos - m.vel.normalized() * trailLen
            trailPaint.alpha = m.alpha / 2
            canvas.drawLine(m.pos.x, m.pos.y, trailEnd.x, trailEnd.y, trailPaint)

            // Missile body (small triangle)
            missilePaint.color = Constants.MISSILE_COLOR.toInt()
            missilePaint.alpha = m.alpha
            val size = 8f
            val cosA = cos(m.angle)
            val sinA = sin(m.angle)
            val tipX = m.pos.x + cosA * size
            val tipY = m.pos.y + sinA * size
            val perpX = -sinA
            val perpY = cosA
            val backLX = m.pos.x - cosA * size * 0.5f + perpX * size * 0.5f
            val backLY = m.pos.y - sinA * size * 0.5f + perpY * size * 0.5f
            val backRX = m.pos.x - cosA * size * 0.5f - perpX * size * 0.5f
            val backRY = m.pos.y - sinA * size * 0.5f - perpY * size * 0.5f

            val path = android.graphics.Path()
            path.moveTo(tipX, tipY)
            path.lineTo(backLX, backLY)
            path.lineTo(backRX, backRY)
            path.close()
            canvas.drawPath(path, missilePaint)
        }
    }
}
