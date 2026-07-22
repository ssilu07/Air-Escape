package com.royals.airescape.screen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import com.royals.airescape.data.Constants
import com.royals.airescape.data.GameData
import com.royals.airescape.data.PlaneAbility
import com.royals.airescape.data.PlaneConfig
import com.royals.airescape.engine.GameSurfaceView
import com.royals.airescape.engine.Renderer
import com.royals.airescape.util.Vector2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Plane selection screen.
 *
 * Displays a grid of plane cards (2 rows of 5). Each card shows a colored
 * plane preview, name, speed modifier, and lock/unlock state. Players can
 * spend stars to unlock planes or tap an unlocked plane to select it.
 */
class PlaneSelectScreen(private val surfaceView: GameSurfaceView) : Screen {

    // ── Data ─────────────────────────────────────────────────────────
    private val planes = PlaneConfig.PLANES

    // ── Layout ───────────────────────────────────────────────────────
    private var screenWidth = 0f
    private var screenHeight = 0f
    private var layoutDone = false

    // ── Card regions (indexed by plane id) ───────────────────────────
    private val cardRects = Array(planes.size) { RectF() }
    private val backButtonRect = RectF()

    // ── Scroll state ──────────────────────────────────────────────
    private var scrollOffset = 0f
    private var totalContentHeight = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    // ── Paints ───────────────────────────────────────────────────────
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
        color = 0xFF000000.toInt()
        alpha = 140
        style = Paint.Style.FILL
    }

    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFBBBBBB.toInt()
        textAlign = Paint.Align.CENTER
    }

    private val costPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Constants.STAR_COLOR.toInt()
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00E676.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val starsHudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Constants.STAR_COLOR.toInt()
        isFakeBoldText = true
        textAlign = Paint.Align.RIGHT
    }

    private val planePath = Path()

    /** Cached plane preview bitmaps keyed by drawable resource id. */
    private val planeBitmapCache = mutableMapOf<Int, Bitmap>()

    override fun onExit() {
        // Recycle cached bitmaps to free GPU/heap memory
        for (bmp in planeBitmapCache.values) {
            bmp.recycle()
        }
        planeBitmapCache.clear()
    }

    private fun getPlanePreviewBitmap(config: PlaneConfig, size: Int): Bitmap? {
        planeBitmapCache[config.drawableRes]?.let { return it }
        return try {
            val drawable = ContextCompat.getDrawable(surfaceView.context, config.drawableRes)
                ?: return null
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c = Canvas(bitmap)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(c)
            planeBitmapCache[config.drawableRes] = bitmap
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    // ── Layout ───────────────────────────────────────────────────────

    // Cached card dimensions for re-layout on scroll
    private var cardW = 0f
    private var cardH = 0f
    private var cardGap = 0f
    private var cardTopMargin = 0f
    private var cardPadding = 0f

    private fun layoutIfNeeded() {
        val sw = GameSurfaceView.screenWidth.toFloat()
        val sh = GameSurfaceView.screenHeight.toFloat()
        if (sw <= 0f || sh <= 0f) return

        screenWidth = sw
        screenHeight = sh

        // Back button (top-left)
        cardPadding = sw * 0.04f
        val backW = sw * 0.2f
        val backH = sh * 0.035f
        backButtonRect.set(cardPadding, sh * 0.015f, cardPadding + backW, sh * 0.015f + backH)

        // Card grid below header (2 columns, rows computed from plane count)
        val cols = 2
        val rows = (planes.size + cols - 1) / cols
        cardTopMargin = sh * 0.1f
        val bottomMargin = sh * 0.02f
        val cardAreaWidth = sw - cardPadding * 2f
        cardGap = sw * 0.03f
        cardW = (cardAreaWidth - (cols - 1) * cardGap) / cols
        cardH = ((sh - cardTopMargin - bottomMargin) - (rows - 1) * cardGap) / rows
        totalContentHeight = cardTopMargin + rows * (cardH + cardGap)

        updateCardPositions()
        layoutDone = true
    }

    private fun updateCardPositions() {
        val cols = 2
        for (i in planes.indices) {
            val col = i % cols
            val row = i / cols
            val x = cardPadding + col * (cardW + cardGap)
            val y = cardTopMargin + row * (cardH + cardGap) + scrollOffset
            cardRects[i].set(x, y, x + cardW, y + cardH)
        }
    }

    // ── Screen interface ─────────────────────────────────────────────

    override fun update(dt: Float) {
        if (!layoutDone) layoutIfNeeded()
    }

    override fun render(canvas: Canvas) {
        if (!layoutDone) return

        canvas.drawColor(Constants.BACKGROUND_COLOR.toInt())

        val padding = screenWidth * 0.04f

        // Back button (top-left)
        Renderer.drawButton(
            canvas, backButtonRect, "BACK",
            color = 0xFF546E7A,
            textColor = 0xFFFFFFFF
        )

        // Title (centered, below back button row)
        titlePaint.textSize = screenWidth * 0.06f
        canvas.drawText("SELECT PLANE", screenWidth / 2f, screenHeight * 0.075f, titlePaint)

        // Total stars (top-right, aligned with back button)
        starsHudPaint.textSize = screenWidth * 0.038f
        val starsText = "\u2605 ${GameData.totalStars}"
        canvas.drawText(starsText, screenWidth - padding, backButtonRect.centerY() + screenWidth * 0.013f, starsHudPaint)

        // Plane cards
        val selectedId = GameData.selectedPlane
        for (i in planes.indices) {
            val plane = planes[i]
            val rect = cardRects[i]
            val unlocked = GameData.isPlaneUnlocked(plane.id)
            val cardH = rect.height()
            val cardW = rect.width()

            // Card background
            cardBgPaint.color = 0xFF2A2A3E.toInt()
            canvas.drawRoundRect(rect, 14f, 14f, cardBgPaint)

            // Plane preview bitmap (upper half of card)
            val previewCenterX = rect.centerX()
            val previewCenterY = rect.top + cardH * 0.32f
            val previewSize = (cardW * 0.35f).coerceAtMost(cardH * 0.38f)
            drawPlanePreview(canvas, previewCenterX, previewCenterY, previewSize, plane)

            // Name
            namePaint.textSize = cardW * 0.15f
            canvas.drawText(plane.name, rect.centerX(), rect.top + cardH * 0.66f, namePaint)

            // Speed modifier
            val speedText = String.format("Speed: %.0f%%", plane.speedModifier * 100)
            detailPaint.textSize = cardW * 0.10f
            canvas.drawText(speedText, rect.centerX(), rect.top + cardH * 0.74f, detailPaint)

            // Ability (if any)
            if (plane.ability != PlaneAbility.NONE) {
                detailPaint.color = 0xFF80CBC4.toInt()
                detailPaint.textSize = cardW * 0.09f
                canvas.drawText(plane.ability.description, rect.centerX(), rect.top + cardH * 0.82f, detailPaint)
                detailPaint.color = 0xFFBBBBBB.toInt()
            } else if (plane.hasGun) {
                detailPaint.color = Constants.BULLET_SHOOT_COLOR.toInt()
                detailPaint.textSize = cardW * 0.09f
                canvas.drawText("Built-in gun", rect.centerX(), rect.top + cardH * 0.82f, detailPaint)
                detailPaint.color = 0xFFBBBBBB.toInt()
            }

            if (!unlocked) {
                // Locked overlay
                canvas.drawRoundRect(rect, 14f, 14f, lockedOverlayPaint)

                // Lock icon and cost
                costPaint.textSize = cardW * 0.15f
                val lockText = "\uD83D\uDD12 ${plane.unlockCost}"
                canvas.drawText(lockText, rect.centerX(), rect.top + cardH * 0.92f, costPaint)
            } else {
                if (plane.id == selectedId) {
                    // Selected border
                    canvas.drawRoundRect(rect, 14f, 14f, selectedPaint)

                    detailPaint.color = 0xFF00E676.toInt()
                    detailPaint.textSize = cardW * 0.12f
                    canvas.drawText("SELECTED", rect.centerX(), rect.top + cardH * 0.91f, detailPaint)
                    detailPaint.color = 0xFFBBBBBB.toInt()
                } else {
                    // Normal unlocked border
                    cardBorderPaint.color = 0xFF555555.toInt()
                    canvas.drawRoundRect(rect, 14f, 14f, cardBorderPaint)

                    detailPaint.textSize = cardW * 0.11f
                    canvas.drawText("TAP TO SELECT", rect.centerX(), rect.top + cardH * 0.91f, detailPaint)
                }
            }
        }
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
                if (kotlin.math.abs(dy) > 10f) isDragging = true
                if (isDragging) {
                    scrollOffset += dy
                    // Clamp scroll
                    val maxScroll = 0f
                    val minScroll = -(totalContentHeight - screenHeight).coerceAtLeast(0f)
                    scrollOffset = scrollOffset.coerceIn(minScroll, maxScroll)
                    lastTouchY = event.y
                    updateCardPositions()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    // It's a tap — check cards
                    for (i in planes.indices) {
                        if (cardRects[i].contains(event.x, event.y)) {
                            val plane = planes[i]
                            if (GameData.isPlaneUnlocked(plane.id)) {
                                GameData.selectedPlane = plane.id
                            } else {
                                if (GameData.totalStars >= plane.unlockCost) {
                                    GameData.totalStars = GameData.totalStars - plane.unlockCost
                                    GameData.unlockPlane(plane.id)
                                    GameData.selectedPlane = plane.id
                                }
                            }
                            return true
                        }
                    }
                }
            }
        }
        return true
    }

    override fun onBackPressed(): Boolean {
        surfaceView.setScreen(MenuScreen(surfaceView))
        return true
    }

    // ── Plane preview drawing ────────────────────────────────────────

    private fun drawPlanePreview(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        config: PlaneConfig
    ) {
        val bitmapSize = (size * 1.4f).toInt().coerceAtLeast(1)
        val bmp = getPlanePreviewBitmap(config, bitmapSize)
        if (bmp != null) {
            canvas.drawBitmap(bmp, cx - bmp.width / 2f, cy - bmp.height / 2f, null)
        } else {
            // Fallback: simple dart/chevron shape
            val angle = -Math.PI.toFloat() / 2f
            val cosA = cos(angle)
            val sinA = sin(angle)
            val perpX = -sinA
            val perpY = cosA

            val noseX = cx + cosA * size
            val noseY = cy + sinA * size
            val wingLX = cx - cosA * size * 0.5f + perpX * size * 0.7f
            val wingLY = cy - sinA * size * 0.5f + perpY * size * 0.7f
            val wingRX = cx - cosA * size * 0.5f - perpX * size * 0.7f
            val wingRY = cy - sinA * size * 0.5f - perpY * size * 0.7f
            val tailX = cx - cosA * size * 0.35f
            val tailY = cy - sinA * size * 0.35f

            planePath.reset()
            planePath.moveTo(noseX, noseY)
            planePath.lineTo(wingLX, wingLY)
            planePath.lineTo(tailX, tailY)
            planePath.lineTo(wingRX, wingRY)
            planePath.close()

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.colorPrimary.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawPath(planePath, paint)
        }
    }
}
