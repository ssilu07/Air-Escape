package com.royals.airescape.screen

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.royals.airescape.ads.AdManager
import com.royals.airescape.data.Constants
import com.royals.airescape.data.GameData
import com.royals.airescape.data.ThemeConfig
import com.royals.airescape.engine.GameSurfaceView
import com.royals.airescape.engine.Renderer

class ThemeSelectScreen(private val surfaceView: GameSurfaceView) : Screen {

    private val themes: List<ThemeConfig> = ThemeConfig.THEMES
    private var screenWidth = 0f
    private var screenHeight = 0f
    private var layoutDone = false

    private val cardRects = Array(themes.size) { RectF() }
    private val backButtonRect = RectF()

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val lockedOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x88000000.toInt()
        style = Paint.Style.FILL
    }

    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val costPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Constants.STAR_COLOR.toInt()
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00E676.toInt()
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFCCCCCC.toInt()
        textAlign = Paint.Align.CENTER
    }

    private val starsHudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Constants.STAR_COLOR.toInt()
        isFakeBoldText = true
        textAlign = Paint.Align.RIGHT
    }

    private val skyPreviewPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val cloudPreviewPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

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

        // 2 columns x 3 rows grid
        val gridTop = sh * 0.12f
        val cols = 2
        val rows = 3
        val colGap = sw * 0.04f
        val rowGap = sh * 0.02f
        val cardW = (sw - padding * 2f - colGap) / cols
        val cardH = (sh * 0.72f - rowGap * 2f) / rows

        for (i in themes.indices) {
            val c = i % cols
            val r = i / cols
            val left = padding + c * (cardW + colGap)
            val top = gridTop + r * (cardH + rowGap)
            cardRects[i].set(left, top, left + cardW, top + cardH)
        }

        layoutDone = true
    }

    override fun update(dt: Float) {
        if (!layoutDone) layoutIfNeeded()
    }

    override fun render(canvas: Canvas) {
        if (!layoutDone) return

        canvas.drawColor(Constants.BACKGROUND_COLOR.toInt())
        val padding = screenWidth * 0.04f

        // Back button
        Renderer.drawButton(canvas, backButtonRect, "BACK", color = 0xFF546E7A, textColor = 0xFFFFFFFF)

        // Title
        titlePaint.textSize = screenWidth * 0.06f
        canvas.drawText("THEMES", screenWidth / 2f, backButtonRect.centerY() + titlePaint.textSize * 0.35f, titlePaint)

        // Stars HUD (top right)
        starsHudPaint.textSize = screenWidth * 0.038f
        val starsText = "\u2605 ${GameData.totalStars}"
        canvas.drawText(starsText, screenWidth - padding, backButtonRect.centerY() + screenWidth * 0.013f, starsHudPaint)

        val selectedThemeId = GameData.selectedTheme

        for (i in themes.indices) {
            val theme = themes[i]
            val rect = cardRects[i]
            val isUnlocked = GameData.isThemeUnlocked(theme.id)
            val isSelected = theme.id == selectedThemeId

            // Card background & preview
            cardBgPaint.color = 0xFF212121.toInt()
            canvas.drawRoundRect(rect, 14f, 14f, cardBgPaint)

            // Sky & Cloud preview area
            val previewH = rect.height() * 0.5f
            val previewRect = RectF(rect.left + 4f, rect.top + 4f, rect.right - 4f, rect.top + previewH)
            skyPreviewPaint.color = theme.skyColor.toInt()
            canvas.drawRoundRect(previewRect, 10f, 10f, skyPreviewPaint)

            cloudPreviewPaint.color = theme.cloudColor.toInt()
            cloudPreviewPaint.alpha = theme.cloudAlpha
            canvas.drawCircle(previewRect.centerX() - 20f, previewRect.centerY(), 18f, cloudPreviewPaint)
            canvas.drawCircle(previewRect.centerX() + 10f, previewRect.centerY() - 4f, 24f, cloudPreviewPaint)
            canvas.drawCircle(previewRect.centerX() + 30f, previewRect.centerY() + 4f, 16f, cloudPreviewPaint)

            // Border
            cardBorderPaint.color = when {
                isSelected -> 0xFF00E676.toInt()
                isUnlocked -> 0xFF555555.toInt()
                else -> 0xFF333333.toInt()
            }
            canvas.drawRoundRect(rect, 14f, 14f, cardBorderPaint)

            // Name
            namePaint.textSize = rect.height() * 0.12f
            canvas.drawText(theme.name, rect.centerX(), rect.top + rect.height() * 0.68f, namePaint)

            // Status / Cost
            detailPaint.textSize = rect.height() * 0.09f
            when {
                isSelected -> {
                    selectedPaint.textSize = rect.height() * 0.10f
                    canvas.drawText("EQUIPPED", rect.centerX(), rect.top + rect.height() * 0.88f, selectedPaint)
                }
                isUnlocked -> {
                    canvas.drawText("TAP TO SELECT", rect.centerX(), rect.top + rect.height() * 0.88f, detailPaint)
                }
                else -> {
                    // Locked overlay
                    canvas.drawRoundRect(rect, 14f, 14f, lockedOverlayPaint)
                    costPaint.textSize = rect.height() * 0.10f
                    canvas.drawText("\u2605 ${theme.unlockCost}", rect.centerX(), rect.top + rect.height() * 0.88f, costPaint)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && layoutDone) {
            val x = event.x
            val y = event.y

            if (backButtonRect.contains(x, y)) {
                surfaceView.setScreen(MenuScreen(surfaceView))
                return true
            }

            for (i in themes.indices) {
                if (cardRects[i].contains(x, y)) {
                    val theme = themes[i]
                    if (GameData.isThemeUnlocked(theme.id)) {
                        GameData.selectedTheme = theme.id
                    } else if (GameData.totalStars >= theme.unlockCost) {
                        GameData.totalStars -= theme.unlockCost
                        GameData.unlockTheme(theme.id)
                        GameData.selectedTheme = theme.id
                    }
                    return true
                }
            }
        }
        return true
    }
}
