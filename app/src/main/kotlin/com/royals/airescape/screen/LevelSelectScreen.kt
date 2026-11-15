package com.royals.airescape.screen

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import com.royals.airescape.data.Constants
import com.royals.airescape.data.GameData
import com.royals.airescape.engine.GameSurfaceView
import com.royals.airescape.engine.Renderer
import com.royals.airescape.level.LevelManager
import com.royals.airescape.util.Vector2
import kotlin.math.abs
import kotlin.math.sin

/**
 * 500-Level Campaign Roadmap Screen.
 * Features natural, smooth 1:1 kinetic scrolling with inertia, momentum damping,
 * virtualized frustum rendering for silky 60/120 FPS performance across all 500 levels,
 * sector milestone banners, boss nodes, and instant navigation back to the current level.
 */
class LevelSelectScreen(
    private val surfaceView: GameSurfaceView
) : Screen {

    private var screenWidth = 0f
    private var screenHeight = 0f
    private var timer = 0f

    // ── Scrolling & Kinetic Physics ─────────────────────────────────────
    private var scrollOffsetY = 0f
    private var targetScrollY = -1f
    private var scrollVelocity = 0f
    private var maxScrollY = 0f
    private var minScrollY = 0f

    private var touchStartY = 0f
    private var lastTouchY = 0f
    private var lastTouchTime = 0L
    private var isDragging = false
    private var touchDownTime = 0L

    // ── Layout Metrics ──────────────────────────────────────────────────
    private val totalLevels = LevelManager.TOTAL_LEVELS // 500 levels
    private val nodeRadius = 38f
    private val nodeSpacing = 120f
    private var startY = 160f

    // Rectangles for header & UI buttons
    private val backButtonRect = RectF()
    private val quickPlayButtonRect = RectF()
    private val focusCurrentButtonRect = RectF()

    // ── Paints ──────────────────────────────────────────────────────────
    private val bgPaint = Paint().apply {
        color = 0xFF070B14.toInt()
        style = Paint.Style.FILL
    }
    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF192A45.toInt()
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val pathActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00E5FF.toInt()
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val nodeStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
    }
    private val nodeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
        color = 0xFFFFFFFF.toInt()
    }
    private val lockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF78909C.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    private val scrollbarTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33FFFFFF.toInt()
        style = Paint.Style.FILL
    }
    private val scrollbarThumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xAA00E5FF.toInt()
        style = Paint.Style.FILL
    }

    private val roadPath = Path()
    private val activeRoadPath = Path()

    init {
        val sw = GameSurfaceView.screenWidth.toFloat()
        val sh = GameSurfaceView.screenHeight.toFloat()
        if (sw > 0f && sh > 0f) {
            setupDimensions(sw, sh)
        }
    }

    private fun setupDimensions(w: Float, h: Float) {
        screenWidth = w
        screenHeight = h
        startY = GameSurfaceView.safeInsetTop + w * 0.18f + 60f
        maxScrollY = (startY + (totalLevels - 1) * nodeSpacing - screenHeight * 0.55f).coerceAtLeast(0f)
        minScrollY = 0f

        // Initial focus on current unlocked level
        val currentI = (GameData.highestLevelUnlocked - 1).coerceIn(0, totalLevels - 1)
        val desiredScroll = (startY + currentI * nodeSpacing - screenHeight * 0.5f).coerceIn(minScrollY, maxScrollY)
        scrollOffsetY = desiredScroll
    }

    override fun update(dt: Float) {
        timer += dt

        // 1. Smooth animation to target level (e.g. when tapping "Jump to Current")
        if (targetScrollY >= 0f) {
            val diff = targetScrollY - scrollOffsetY
            if (abs(diff) > 2f) {
                scrollOffsetY += diff * 12f * dt
            } else {
                scrollOffsetY = targetScrollY
                targetScrollY = -1f
            }
        }
        // 2. Kinetic momentum fling when not dragging
        else if (!isDragging) {
            if (abs(scrollVelocity) > 15f) {
                scrollOffsetY += scrollVelocity * dt
                // Exponential decay damping
                scrollVelocity *= (1f - 5f * dt).coerceIn(0f, 1f)
            } else {
                scrollVelocity = 0f
            }

            // 3. Elastic bounce-back at top/bottom bounds
            if (scrollOffsetY < minScrollY) {
                scrollOffsetY += (minScrollY - scrollOffsetY) * 12f * dt
                scrollVelocity *= 0.5f
            } else if (scrollOffsetY > maxScrollY) {
                scrollOffsetY += (maxScrollY - scrollOffsetY) * 12f * dt
                scrollVelocity *= 0.5f
            }
        }
    }

    override fun render(canvas: Canvas) {
        if (screenWidth <= 0f || screenHeight <= 0f) {
            val sw = GameSurfaceView.screenWidth.toFloat()
            val sh = GameSurfaceView.screenHeight.toFloat()
            if (sw > 0f && sh > 0f) {
                setupDimensions(sw, sh)
            } else {
                setupDimensions(canvas.width.toFloat(), canvas.height.toFloat())
            }
        }

        // 1. Space background
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, bgPaint)

        val cx = screenWidth / 2f
        val highest = GameData.highestLevelUnlocked

        // Calculate visible index range (Frustum virtual culling for 500 levels)
        // ny = startY + i * nodeSpacing - scrollOffsetY
        val buffer = 200f
        val minI = (((scrollOffsetY - startY - buffer) / nodeSpacing).toInt()).coerceIn(0, totalLevels - 1)
        val maxI = (((screenHeight + scrollOffsetY - startY + buffer) / nodeSpacing).toInt()).coerceIn(0, totalLevels - 1)

        // 2. Draw connecting winding road between visible nodes
        roadPath.reset()
        activeRoadPath.reset()

        val roadStartI = (minI - 1).coerceAtLeast(0)
        val roadEndI = (maxI + 1).coerceAtMost(totalLevels - 1)

        var roadStarted = false
        var activeStarted = false

        for (i in roadStartI..roadEndI) {
            val lvl = i + 1
            val nx = cx + sin(i * 0.85f) * (screenWidth * 0.28f)
            val ny = startY + i * nodeSpacing - scrollOffsetY

            if (!roadStarted) {
                roadPath.moveTo(nx, ny)
                roadStarted = true
            } else {
                roadPath.lineTo(nx, ny)
            }

            if (lvl <= highest) {
                if (!activeStarted) {
                    activeRoadPath.moveTo(nx, ny)
                    activeStarted = true
                } else {
                    activeRoadPath.lineTo(nx, ny)
                }
            }
        }
        canvas.drawPath(roadPath, pathPaint)
        if (activeStarted) {
            canvas.drawPath(activeRoadPath, pathActivePaint)
        }

        // 3. Draw Sector Banners & Level Nodes
        for (i in minI..maxI) {
            val lvl = i + 1
            val nx = cx + sin(i * 0.85f) * (screenWidth * 0.28f)
            val ny = startY + i * nodeSpacing - scrollOffsetY

            // Sector Milestone Banner (every 10 levels)
            if (lvl % 10 == 1 && lvl > 1) {
                val sectorNum = (lvl - 1) / 10 + 1
                val bannerY = ny - nodeSpacing * 0.52f
                val bannerW = screenWidth * 0.62f
                val bannerH = 26f
                val bannerRect = RectF(cx - bannerW / 2f, bannerY - bannerH / 2f, cx + bannerW / 2f, bannerY + bannerH / 2f)
                Renderer.drawGlassPanel(canvas, bannerRect, 0xFF00E5FF.toInt(), 13f, 0xDD0D1B2A.toInt(), 0xDD1B263B.toInt())
                Renderer.drawShadowText(canvas, "— SECTOR $sectorNum —", cx, bannerY + 5f, 13f, 0xFF81D4FA.toInt(), Paint.Align.CENTER)
            }

            val isUnlocked = lvl <= highest
            val isCurrent = lvl == highest
            val isMajorBoss = (lvl % 50 == 0)
            val isBoss = (lvl % 10 == 0 || lvl == 5)
            val stars = GameData.getLevelStars(lvl)

            val r = when {
                isMajorBoss -> nodeRadius * 1.35f
                isBoss -> nodeRadius * 1.2f
                else -> nodeRadius
            }

            if (isUnlocked) {
                // Node Body
                val bodyColor = when {
                    isMajorBoss -> 0xFF880E4F.toInt()
                    isBoss -> 0xFF4A148C.toInt()
                    isCurrent -> 0xFF006064.toInt()
                    else -> 0xFF0D47A1.toInt()
                }
                nodePaint.color = bodyColor
                canvas.drawCircle(nx, ny, r, nodePaint)

                // Outer Rim
                val rimColor = when {
                    isMajorBoss -> 0xFFFFD700.toInt()
                    isBoss -> 0xFFFF1744.toInt()
                    isCurrent -> 0xFF00E5FF.toInt()
                    else -> 0xFF4FC3F7.toInt()
                }
                nodeStroke.color = rimColor
                canvas.drawCircle(nx, ny, r, nodeStroke)

                // Pulsing Halo for Current Unlocked Level
                if (isCurrent) {
                    val pulse = 1f + 0.15f * sin(timer * 6f).toFloat()
                    val haloPaint = Paint(nodeStroke).apply {
                        strokeWidth = 2.5f
                        alpha = ((0.7f - 0.3f * (pulse - 1f)) * 255).toInt().coerceIn(0, 255)
                    }
                    canvas.drawCircle(nx, ny, r * pulse * 1.25f, haloPaint)
                }

                // Level Number / Boss Icon
                if (isMajorBoss) {
                    nodeTextPaint.textSize = r * 0.75f
                    canvas.drawText("💀", nx, ny + r * 0.3f, nodeTextPaint)
                } else if (isBoss) {
                    nodeTextPaint.textSize = r * 0.75f
                    canvas.drawText("👑", nx, ny + r * 0.3f, nodeTextPaint)
                } else {
                    nodeTextPaint.textSize = if (lvl >= 100) r * 0.65f else r * 0.75f
                    canvas.drawText("$lvl", nx, ny + r * 0.28f, nodeTextPaint)
                }

                // Star Badges under the node
                val starY = ny + r + 14f
                val starR = 6.5f
                for (s in 1..3) {
                    val sx = nx + (s - 2) * 16f
                    val starEarned = s <= stars
                    Renderer.drawStar(
                        canvas, Vector2(sx, starY),
                        starR, 5,
                        if (starEarned) Constants.STAR_COLOR else 0x44FFFFFF
                    )
                }

                // "PLAY" Pill over Current Level
                if (isCurrent) {
                    val pillW = 60f
                    val pillH = 22f
                    val pillRect = RectF(nx - pillW / 2f, ny - r - 26f, nx + pillW / 2f, ny - r - 4f)
                    Renderer.drawGlassPanel(canvas, pillRect, 0xFF00E676.toInt(), pillH / 2f, 0xFF00E676.toInt(), 0xFF00B0FF.toInt())
                    Renderer.drawShadowText(canvas, "PLAY", nx, ny - r - 9f, 13f, 0xFF002211.toInt(), Paint.Align.CENTER)
                }

            } else {
                // Locked Node
                nodePaint.color = 0xFF141E30.toInt()
                canvas.drawCircle(nx, ny, r, nodePaint)
                nodeStroke.color = 0xFF37474F.toInt()
                canvas.drawCircle(nx, ny, r, nodeStroke)

                // Padlock Icon
                canvas.drawRoundRect(RectF(nx - 8f, ny - 2f, nx + 8f, ny + 10f), 3f, 3f, lockPaint)
                canvas.drawArc(RectF(nx - 6f, ny - 10f, nx + 6f, ny + 2f), 180f, 180f, false, lockPaint)

                // Locked Level Number
                val smallPaint = Paint(nodeTextPaint).apply {
                    textSize = 13f
                    color = 0xFF78909C.toInt()
                }
                canvas.drawText("$lvl", nx, ny + r + 15f, smallPaint)
            }
        }

        // 4. Fast Scrollbar on Right Edge (Mini-map thumb)
        renderScrollbar(canvas, minI + 1)

        // 5. Fixed Top Header
        renderHeader(canvas)

        // 6. Floating Action Buttons (Quick Play & Jump to Current)
        renderFloatingButtons(canvas)
    }

    private fun renderScrollbar(canvas: Canvas, currentViewingLevel: Int) {
        if (maxScrollY <= 0f) return
        val barW = 5f
        val barX = screenWidth - 12f
        val barTop = screenHeight * 0.18f
        val barH = screenHeight * 0.65f

        // Track
        canvas.drawRoundRect(RectF(barX, barTop, barX + barW, barTop + barH), 2.5f, 2.5f, scrollbarTrackPaint)

        // Thumb
        val progress = (scrollOffsetY / maxScrollY).coerceIn(0f, 1f)
        val thumbH = 30f
        val thumbY = barTop + progress * (barH - thumbH)
        canvas.drawRoundRect(RectF(barX - 1f, thumbY, barX + barW + 1f, thumbY + thumbH), 3f, 3f, scrollbarThumbPaint)

        // Floating level chip next to thumb during drag
        if (isDragging || abs(scrollVelocity) > 200f) {
            val chipW = 60f
            val chipH = 20f
            val chipRect = RectF(barX - chipW - 6f, thumbY + 5f, barX - 6f, thumbY + 5f + chipH)
            Renderer.drawGlassPanel(canvas, chipRect, 0xFF00E5FF.toInt(), 10f, 0xEE0A192F.toInt(), 0xEE020C1B.toInt())
            Renderer.drawShadowText(canvas, "L$currentViewingLevel", chipRect.centerX(), chipRect.centerY() + 4f, 11f, 0xFF00E5FF.toInt(), Paint.Align.CENTER)
        }
    }

    private fun renderHeader(canvas: Canvas) {
        val safeTop = GameSurfaceView.safeInsetTop
        val barH = screenWidth * 0.16f
        val headerRect = RectF(0f, 0f, screenWidth, safeTop + barH)

        Renderer.drawGlassPanel(canvas, headerRect, 0xFF00E5FF.toInt(), 0f, 0xF0070B14.toInt(), 0xF00A1528.toInt())

        val topY = safeTop + 14f

        // Back Button
        val btnSize = barH * 0.6f
        backButtonRect.set(20f, topY, 20f + btnSize * 1.5f, topY + btnSize)
        Renderer.drawGlossyButton(canvas, backButtonRect, "◀ MENU", 0xFF37474F.toInt(), 0xFFFFFFFF.toInt())

        // Header Title
        Renderer.drawShadowText(
            canvas, "CAMPAIGN (1-500)",
            screenWidth / 2f, topY + btnSize * 0.45f,
            screenWidth * 0.042f, 0xFFFFFFFF.toInt(), Paint.Align.CENTER
        )

        // Stars & Coins Badges (Right side)
        val totalStars = GameData.totalCampaignStars
        val starText = "★ $totalStars"
        val coinText = "🪙 ${GameData.totalCoins}"

        Renderer.drawShadowText(
            canvas, starText,
            screenWidth - 20f, topY + btnSize * 0.35f,
            screenWidth * 0.034f, Constants.STAR_COLOR.toInt(), Paint.Align.RIGHT
        )
        Renderer.drawShadowText(
            canvas, coinText,
            screenWidth - 20f, topY + btnSize * 0.85f,
            screenWidth * 0.030f, 0xFFFFD700.toInt(), Paint.Align.RIGHT
        )
    }

    private fun renderFloatingButtons(canvas: Canvas) {
        val highest = GameData.highestLevelUnlocked
        val currentI = (highest - 1).coerceIn(0, totalLevels - 1)
        val currentLevelScrollY = (startY + currentI * nodeSpacing - screenHeight * 0.5f).coerceIn(minScrollY, maxScrollY)
        val isFarFromCurrent = abs(scrollOffsetY - currentLevelScrollY) > screenHeight * 0.6f

        // Quick Play Button
        val qpW = if (isFarFromCurrent) screenWidth * 0.54f else screenWidth * 0.70f
        val qpH = screenHeight * 0.055f
        val qpX = if (isFarFromCurrent) screenWidth * 0.05f else (screenWidth - qpW) / 2f
        val qpY = screenHeight - qpH - 30f
        quickPlayButtonRect.set(qpX, qpY, qpX + qpW, qpY + qpH)
        Renderer.drawGlossyButton(
            canvas, quickPlayButtonRect,
            "PLAY LEVEL $highest  ▶",
            0xFF00E676.toInt(), 0xFF04210F.toInt()
        )

        // "Jump to Current Level" Button (only when user has scrolled far away)
        if (isFarFromCurrent) {
            val fcW = screenWidth * 0.34f
            val fcX = screenWidth * 0.61f
            focusCurrentButtonRect.set(fcX, qpY, fcX + fcW, qpY + qpH)
            Renderer.drawGlossyButton(
                canvas, focusCurrentButtonRect,
                "🎯 CURRENT",
                0xFF00B0FF.toInt(), 0xFF021B2F.toInt()
            )
        } else {
            focusCurrentButtonRect.setEmpty()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartY = event.y
                lastTouchY = event.y
                lastTouchTime = System.currentTimeMillis()
                touchDownTime = System.currentTimeMillis()
                scrollVelocity = 0f
                targetScrollY = -1f
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - touchStartY
                val now = System.currentTimeMillis()
                val dtTouch = ((now - lastTouchTime) / 1000f).coerceAtLeast(0.001f)
                val instantVelocity = (event.y - lastTouchY) / dtTouch
                lastTouchY = event.y
                lastTouchTime = now

                if (abs(dy) > 10f || isDragging) {
                    isDragging = true
                    // Natural 1:1 finger tracking:
                    // Finger moves UP (event.y decreases) -> delta is negative -> scrollOffsetY increases -> ny decreases (levels scroll UP)!
                    // Finger moves DOWN (event.y increases) -> delta is positive -> scrollOffsetY decreases -> ny increases (levels scroll DOWN)!
                    val deltaY = event.y - touchStartY
                    scrollOffsetY -= deltaY
                    touchStartY = event.y

                    // Smooth kinetic velocity tracking
                    val flingV = -instantVelocity
                    scrollVelocity = scrollVelocity * 0.35f + flingV * 0.65f
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val clickDuration = System.currentTimeMillis() - touchDownTime
                val x = event.x
                val y = event.y

                // If it was a quick tap without drag
                if (!isDragging && clickDuration < 300) {
                    // Check Back button
                    if (backButtonRect.contains(x, y)) {
                        surfaceView.setScreen(MenuScreen(surfaceView))
                        return true
                    }

                    // Check Jump to Current button
                    if (!focusCurrentButtonRect.isEmpty && focusCurrentButtonRect.contains(x, y)) {
                        val currentI = (GameData.highestLevelUnlocked - 1).coerceIn(0, totalLevels - 1)
                        targetScrollY = (startY + currentI * nodeSpacing - screenHeight * 0.5f).coerceIn(minScrollY, maxScrollY)
                        return true
                    }

                    // Check Quick Play button
                    if (quickPlayButtonRect.contains(x, y)) {
                        val highest = GameData.highestLevelUnlocked
                        val config = LevelManager.getLevel(highest)
                        surfaceView.setScreen(GameScreen(surfaceView, levelConfig = config))
                        return true
                    }

                    // Check Level Node clicks (within visible range)
                    val cx = screenWidth / 2f
                    val highest = GameData.highestLevelUnlocked

                    val buffer = 150f
                    val minI = (((scrollOffsetY - startY - buffer) / nodeSpacing).toInt()).coerceIn(0, totalLevels - 1)
                    val maxI = (((screenHeight + scrollOffsetY - startY + buffer) / nodeSpacing).toInt()).coerceIn(0, totalLevels - 1)

                    for (i in minI..maxI) {
                        val lvl = i + 1
                        if (lvl > highest) continue // cannot click locked levels

                        val nx = cx + sin(i * 0.85f) * (screenWidth * 0.28f)
                        val ny = startY + i * nodeSpacing - scrollOffsetY
                        val r = if (lvl % 10 == 0 || lvl == 5) nodeRadius * 1.3f else nodeRadius

                        val distSq = (x - nx) * (x - nx) + (y - ny) * (y - ny)
                        if (distSq <= (r * 1.5f) * (r * 1.5f)) {
                            // Unlocked level tapped! Launch it!
                            val config = LevelManager.getLevel(lvl)
                            surfaceView.setScreen(GameScreen(surfaceView, levelConfig = config))
                            return true
                        }
                    }
                }
                isDragging = false
                return true
            }
        }
        return true
    }

    override fun onSizeChanged(width: Float, height: Float) {
        setupDimensions(width, height)
    }

    override fun onBackPressed(): Boolean {
        surfaceView.setScreen(MenuScreen(surfaceView))
        return true
    }
}
