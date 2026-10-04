package com.royals.airescape.screen

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import com.royals.airescape.ads.AdManager
import com.royals.airescape.data.Constants
import com.royals.airescape.engine.GameSurfaceView
import com.royals.airescape.engine.Renderer
import kotlin.math.abs

class MissileInfoScreen(private val surfaceView: GameSurfaceView) : Screen {

    data class MissileInfo(
        val name: String,
        val color: Long,
        val symbol: String,
        val tagline: String,
        val details: List<String>
    )

    private val missiles: List<MissileInfo> = listOf(
        MissileInfo(
            "HOMING", 0xFFFF1744, "H",
            "Tracks your plane steadily",
            listOf("Steers toward your plane", "Ramps up speed over time", "Guide them into collisions!")
        ),
        MissileInfo(
            "STRAIGHT", 0xFFFF9100, "S",
            "High-speed rocket",
            listOf("Moves at ultra-fast speeds", "Cannot steer or change direction", "Dodge quickly as it launches!")
        ),
        MissileInfo(
            "BOUNCING", 0xFFFFEA00, "B",
            "Edge ricochet missile",
            listOf("Bounces off screen edges", "Retains speed on each bounce", "Creates chaotic criss-cross patterns")
        ),
        MissileInfo(
            "CLUSTER", 0xFFAA00FF, "C",
            "Splits on destruction",
            listOf("Normal tracking behavior", "Splits into 3 mini missiles on death", "Clear out immediately after impact!")
        ),
        MissileInfo(
            "STEALTH", 0xFF00E5FF, "T",
            "Invisibility cloak",
            listOf("Periodically vanishes from sight", "Still continues tracking your plane", "Predict its path while hidden")
        ),
        MissileInfo(
            "BOSS", 0xFFD50000, "★",
            "Giant titan missile",
            listOf("Takes 5 hits to destroy", "Displays live health bar", "Gives +200 bonus score on defeat")
        )
    )

    private var screenWidth = 0f
    private var screenHeight = 0f
    private var layoutDone = false

    private val backButtonRect = RectF()
    private val cardRects = Array(missiles.size) { RectF() }

    private var scrollOffset = 0f
    private var totalContentHeight = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    private var cardPadding = 0f
    private var cardW = 0f
    private var cardH = 0f
    private var cardGap = 0f
    private var topMargin = 0f

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1E1E2E.toInt()
        style = Paint.Style.FILL
    }

    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFakeBoldText = true
    }

    private val taglinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFBBBBBB.toInt()
    }

    private val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF888888.toInt()
    }

    private val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val missilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val missilePath = Path()

    override fun onEnter() {
        AdManager.showBanner()
    }

    override fun onExit() {
        AdManager.hideBanner()
    }

    override fun onBackPressed(): Boolean {
        surfaceView.setScreen(MenuScreen(surfaceView))
        return true
    }

    private fun layoutIfNeeded() {
        val sw = GameSurfaceView.screenWidth.toFloat()
        val sh = GameSurfaceView.screenHeight.toFloat()
        if (sw <= 0f || sh <= 0f) return

        screenWidth = sw
        screenHeight = sh

        val padding = sw * 0.04f
        val backBtnW = sw * 0.22f
        val backBtnH = sh * 0.045f
        backButtonRect.set(padding, padding, padding + backBtnW, padding + backBtnH)

        cardPadding = padding
        cardW = sw - padding * 2f
        cardH = sh * 0.14f
        cardGap = sh * 0.015f
        topMargin = sh * 0.12f

        totalContentHeight = topMargin + missiles.size * (cardH + cardGap) + sh * 0.05f

        updateCardPositions()
        layoutDone = true
    }

    private fun updateCardPositions() {
        for (i in missiles.indices) {
            val top = topMargin + scrollOffset + i * (cardH + cardGap)
            cardRects[i].set(cardPadding, top, cardPadding + cardW, top + cardH)
        }
    }

    override fun update(dt: Float) {
        if (!layoutDone) layoutIfNeeded()
    }

    override fun render(canvas: Canvas) {
        if (!layoutDone) return

        canvas.drawColor(Constants.BACKGROUND_COLOR.toInt())

        // Render cards
        for (i in missiles.indices) {
            val rect = cardRects[i]
            if (rect.bottom >= topMargin && rect.top <= screenHeight) {
                drawMissileCard(canvas, rect, missiles[i])
            }
        }

        // Header overlay background (sticky top)
        val headerPaint = Paint().apply {
            color = Constants.BACKGROUND_COLOR.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, screenWidth, topMargin - 10f, headerPaint)

        // Back button & Title
        Renderer.drawButton(canvas, backButtonRect, "BACK", color = 0xFF546E7A, textColor = 0xFFFFFFFF)

        titlePaint.textSize = screenWidth * 0.055f
        canvas.drawText("MISSILE GUIDE", screenWidth / 2f, backButtonRect.centerY() + titlePaint.textSize * 0.35f, titlePaint)
    }

    private fun drawMissileCard(canvas: Canvas, rect: RectF, info: MissileInfo) {
        // Background card
        canvas.drawRoundRect(rect, 14f, 14f, cardBgPaint)

        // Accent indicator bar on left
        val barPaint = Paint().apply {
            color = info.color.toInt()
            style = Paint.Style.FILL
        }
        val barRect = RectF(rect.left, rect.top, rect.left + 8f, rect.bottom)
        canvas.drawRoundRect(barRect, 8f, 8f, barPaint)

        // Missile Icon
        val iconCx = rect.left + rect.height() * 0.42f
        val iconCy = rect.centerY()
        drawMissileIcon(canvas, iconCx, iconCy, rect.height() * 0.24f, info.color)

        // Name
        val textLeft = rect.left + rect.height() * 0.85f
        namePaint.color = info.color.toInt()
        namePaint.textSize = rect.height() * 0.22f
        canvas.drawText(info.name, textLeft, rect.top + rect.height() * 0.32f, namePaint)

        // Tagline
        taglinePaint.textSize = rect.height() * 0.14f
        canvas.drawText(info.tagline, textLeft, rect.top + rect.height() * 0.52f, taglinePaint)

        // Bullets / Details
        detailPaint.textSize = rect.height() * 0.12f
        val bulletText = info.details.joinToString(" • ")
        canvas.drawText(bulletText, textLeft, rect.top + rect.height() * 0.78f, detailPaint)
    }

    private fun drawMissileIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Long) {
        missilePaint.color = color.toInt()
        missilePath.reset()
        missilePath.moveTo(cx + size, cy)
        missilePath.lineTo(cx - size * 0.7f, cy - size * 0.6f)
        missilePath.lineTo(cx - size * 0.7f, cy + size * 0.6f)
        missilePath.close()
        canvas.drawPath(missilePath, missilePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!layoutDone) return true
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchY = event.y
                isDragging = false
                if (backButtonRect.contains(event.x, event.y)) {
                    surfaceView.setScreen(MenuScreen(surfaceView))
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - lastTouchY
                if (abs(dy) > 10f) {
                    isDragging = true
                }
                if (isDragging) {
                    scrollOffset += dy
                    val minScroll = -(totalContentHeight - screenHeight).coerceAtLeast(0f)
                    scrollOffset = scrollOffset.coerceIn(minScroll, 0f)
                    lastTouchY = event.y
                    updateCardPositions()
                }
            }
        }
        return true
    }
}
