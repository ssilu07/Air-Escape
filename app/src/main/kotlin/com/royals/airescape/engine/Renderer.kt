package com.royals.airescape.engine

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.royals.airescape.util.Vector2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Canvas rendering utilities.
 * All [Paint] objects are allocated once and reused to avoid GC pressure.
 */
object Renderer {

    // Reusable Paint instances
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isFakeBoldText = true
    }

    private val reusablePath = Path()

    // ---- Text ----

    fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Long,
        align: Paint.Align = Paint.Align.LEFT
    ) {
        textPaint.textSize = size
        textPaint.color = color.toInt()
        textPaint.textAlign = align
        canvas.drawText(text, x, y, textPaint)
    }

    // ---- Circle ----

    fun drawCircle(
        canvas: Canvas,
        center: Vector2,
        radius: Float,
        color: Long,
        style: Paint.Style = Paint.Style.FILL
    ) {
        val paint = if (style == Paint.Style.FILL) fillPaint else strokePaint
        paint.color = color.toInt()
        paint.style = style
        canvas.drawCircle(center.x, center.y, radius, paint)
    }

    // ---- Star shape polygon ----

    fun drawStar(
        canvas: Canvas,
        center: Vector2,
        radius: Float,
        points: Int,
        color: Long
    ) {
        if (points < 2) return
        fillPaint.color = color.toInt()
        fillPaint.style = Paint.Style.FILL

        reusablePath.reset()
        val innerRadius = radius * 0.45f
        val totalPoints = points * 2
        val angleStep = (Math.PI * 2.0 / totalPoints).toFloat()
        // Start from top (negative Y is up on screen)
        val startAngle = (-Math.PI / 2.0).toFloat()

        for (i in 0 until totalPoints) {
            val r = if (i % 2 == 0) radius else innerRadius
            val angle = startAngle + i * angleStep
            val px = center.x + cos(angle) * r
            val py = center.y + sin(angle) * r
            if (i == 0) {
                reusablePath.moveTo(px, py)
            } else {
                reusablePath.lineTo(px, py)
            }
        }
        reusablePath.close()
        canvas.drawPath(reusablePath, fillPaint)
    }

    // ---- Triangle (for missiles) ----

    /**
     * Draws a filled triangle pointing in the direction of [angle] (radians).
     * The triangle's tip points forward.
     */
    fun drawTriangle(
        canvas: Canvas,
        center: Vector2,
        angle: Float,
        width: Float,
        height: Float,
        color: Long
    ) {
        fillPaint.color = color.toInt()
        fillPaint.style = Paint.Style.FILL

        val cosA = cos(angle)
        val sinA = sin(angle)

        // Tip (front)
        val tipX = center.x + cosA * height / 2f
        val tipY = center.y + sinA * height / 2f

        // Perpendicular direction for the base
        val perpX = -sinA
        val perpY = cosA

        // Back-left
        val blX = center.x - cosA * height / 2f + perpX * width / 2f
        val blY = center.y - sinA * height / 2f + perpY * width / 2f

        // Back-right
        val brX = center.x - cosA * height / 2f - perpX * width / 2f
        val brY = center.y - sinA * height / 2f - perpY * width / 2f

        reusablePath.reset()
        reusablePath.moveTo(tipX, tipY)
        reusablePath.lineTo(blX, blY)
        reusablePath.lineTo(brX, brY)
        reusablePath.close()
        canvas.drawPath(reusablePath, fillPaint)
    }

    // ---- Plane shape (player) ----

    /**
     * Draws a plane-like shape rotated by [angle].
     * [planeType] selects between different silhouettes:
     *   0 - simple dart / chevron
     *   1 - wider jet shape
     *   2 - rounded dot (fallback)
     */
    fun drawPlane(
        canvas: Canvas,
        center: Vector2,
        angle: Float,
        size: Float,
        color: Long,
        planeType: Int = 0
    ) {
        fillPaint.color = color.toInt()
        fillPaint.style = Paint.Style.FILL

        val cosA = cos(angle)
        val sinA = sin(angle)
        val perpX = -sinA
        val perpY = cosA

        reusablePath.reset()

        when (planeType) {
            0 -> {
                // Dart / chevron
                val nose = Vector2(
                    center.x + cosA * size,
                    center.y + sinA * size
                )
                val wingL = Vector2(
                    center.x - cosA * size * 0.5f + perpX * size * 0.7f,
                    center.y - sinA * size * 0.5f + perpY * size * 0.7f
                )
                val wingR = Vector2(
                    center.x - cosA * size * 0.5f - perpX * size * 0.7f,
                    center.y - sinA * size * 0.5f - perpY * size * 0.7f
                )
                val tail = Vector2(
                    center.x - cosA * size * 0.35f,
                    center.y - sinA * size * 0.35f
                )
                reusablePath.moveTo(nose.x, nose.y)
                reusablePath.lineTo(wingL.x, wingL.y)
                reusablePath.lineTo(tail.x, tail.y)
                reusablePath.lineTo(wingR.x, wingR.y)
                reusablePath.close()
            }
            1 -> {
                // Wider jet
                val nose = Vector2(
                    center.x + cosA * size * 1.1f,
                    center.y + sinA * size * 1.1f
                )
                val midL = Vector2(
                    center.x + perpX * size * 0.35f,
                    center.y + perpY * size * 0.35f
                )
                val midR = Vector2(
                    center.x - perpX * size * 0.35f,
                    center.y - perpY * size * 0.35f
                )
                val wingL = Vector2(
                    center.x - cosA * size * 0.3f + perpX * size * 0.9f,
                    center.y - sinA * size * 0.3f + perpY * size * 0.9f
                )
                val wingR = Vector2(
                    center.x - cosA * size * 0.3f - perpX * size * 0.9f,
                    center.y - sinA * size * 0.3f - perpY * size * 0.9f
                )
                val tailL = Vector2(
                    center.x - cosA * size * 0.8f + perpX * size * 0.4f,
                    center.y - sinA * size * 0.8f + perpY * size * 0.4f
                )
                val tailR = Vector2(
                    center.x - cosA * size * 0.8f - perpX * size * 0.4f,
                    center.y - sinA * size * 0.8f - perpY * size * 0.4f
                )
                reusablePath.moveTo(nose.x, nose.y)
                reusablePath.lineTo(midL.x, midL.y)
                reusablePath.lineTo(wingL.x, wingL.y)
                reusablePath.lineTo(tailL.x, tailL.y)
                reusablePath.lineTo(tailR.x, tailR.y)
                reusablePath.lineTo(wingR.x, wingR.y)
                reusablePath.lineTo(midR.x, midR.y)
                reusablePath.close()
            }
            else -> {
                // Fallback: simple circle
                canvas.drawCircle(center.x, center.y, size, fillPaint)
                return
            }
        }
        canvas.drawPath(reusablePath, fillPaint)
    }

    // ---- Button ----

    fun drawButton(
        canvas: Canvas,
        rect: RectF,
        text: String,
        color: Long,
        textColor: Long
    ) {
        fillPaint.color = color.toInt()
        fillPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, 16f, 16f, fillPaint)

        val textSize = (rect.height() * 0.45f).coerceAtMost(64f)
        textPaint.textSize = textSize
        textPaint.color = textColor.toInt()
        textPaint.textAlign = Paint.Align.CENTER

        // Vertically centre the text inside the rect
        val textY = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(text, rect.centerX(), textY, textPaint)
    }

    // ---- Coin shape ----

    fun drawCoin(
        canvas: Canvas,
        center: Vector2,
        radius: Float
    ) {
        // Outer gold circle
        fillPaint.color = 0xFFFFD700.toInt()
        fillPaint.style = Paint.Style.FILL
        canvas.drawCircle(center.x, center.y, radius, fillPaint)

        // Inner ridge
        strokePaint.color = 0xFFFFA000.toInt()
        strokePaint.strokeWidth = (radius * 0.16f).coerceAtLeast(1.5f)
        canvas.drawCircle(center.x, center.y, radius * 0.78f, strokePaint)

        // ₹ symbol
        textPaint.textSize = radius * 1.05f
        textPaint.color = 0xFF5D4037.toInt()
        textPaint.textAlign = Paint.Align.CENTER
        val yOffset = (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText("\u20B9", center.x, center.y - yOffset, textPaint)
    }

    // ---- Advanced Visual Effects for AAA Screenshots ----

    /**
     * Draws an animated hexagonal force field / energy shield dome.
     */
    fun drawHexagonShield(
        canvas: Canvas,
        center: Vector2,
        radius: Float,
        pulseTimer: Float,
        shieldColor: Int = 0xFF00E5FF.toInt()
    ) {
        // Outer pulsing energy glow
        val pulse = 1f + 0.06f * sin(pulseTimer * 6.0).toFloat()
        val curRadius = radius * pulse

        // Semi-transparent inner dome
        fillPaint.color = shieldColor
        fillPaint.alpha = 38
        fillPaint.style = Paint.Style.FILL
        canvas.drawCircle(center.x, center.y, curRadius, fillPaint)

        // Glowing outer boundary
        strokePaint.color = shieldColor
        strokePaint.strokeWidth = 3.5f
        strokePaint.alpha = (180 + 60 * sin(pulseTimer * 6.0)).toInt().coerceIn(0, 255)
        strokePaint.style = Paint.Style.STROKE
        canvas.drawCircle(center.x, center.y, curRadius, strokePaint)

        // Hexagonal lattice pattern lines across the shield boundary
        val hexCount = 6
        val hexAngleStep = (Math.PI * 2.0 / hexCount).toFloat()
        val rotOffset = pulseTimer * 0.5f

        reusablePath.reset()
        for (i in 0 until hexCount) {
            val a = rotOffset + i * hexAngleStep
            val hx = center.x + cos(a) * curRadius * 0.95f
            val hy = center.y + sin(a) * curRadius * 0.95f
            if (i == 0) reusablePath.moveTo(hx, hy) else reusablePath.lineTo(hx, hy)
        }
        reusablePath.close()
        strokePaint.strokeWidth = 1.8f
        strokePaint.alpha = 110
        canvas.drawPath(reusablePath, strokePaint)
    }

    /**
     * Draws a flickering rocket or jet afterburner exhaust flame behind an entity.
     */
    fun drawExhaustFlame(
        canvas: Canvas,
        center: Vector2,
        angle: Float,
        flameLength: Float,
        flameWidth: Float,
        isBoost: Boolean = false
    ) {
        val backAngle = angle + Math.PI.toFloat()
        val perpX = -sin(angle)
        val perpY = cos(angle)

        // Outer flame (Orange / Red / Cyan if boost)
        val outerColor = if (isBoost) 0xFF00E5FF.toInt() else 0xFFFF3D00.toInt()
        val innerColor = if (isBoost) 0xFFFFFFFF.toInt() else 0xFFFFEA00.toInt()

        // Outer flame triangle
        fillPaint.color = outerColor
        fillPaint.alpha = 230
        fillPaint.style = Paint.Style.FILL

        val tipX = center.x + cos(backAngle) * flameLength
        val tipY = center.y + sin(backAngle) * flameLength
        val baseL_X = center.x + perpX * flameWidth * 0.5f
        val baseL_Y = center.y + perpY * flameWidth * 0.5f
        val baseR_X = center.x - perpX * flameWidth * 0.5f
        val baseR_Y = center.y - perpY * flameWidth * 0.5f

        reusablePath.reset()
        reusablePath.moveTo(baseL_X, baseL_Y)
        reusablePath.lineTo(tipX, tipY)
        reusablePath.lineTo(baseR_X, baseR_Y)
        reusablePath.close()
        canvas.drawPath(reusablePath, fillPaint)

        // Inner hot core flame (smaller, brighter)
        fillPaint.color = innerColor
        fillPaint.alpha = 255
        val innerTipX = center.x + cos(backAngle) * flameLength * 0.6f
        val innerTipY = center.y + sin(backAngle) * flameLength * 0.6f
        val innerBaseL_X = center.x + perpX * flameWidth * 0.25f
        val innerBaseL_Y = center.y + perpY * flameWidth * 0.25f
        val innerBaseR_X = center.x - perpX * flameWidth * 0.25f
        val innerBaseR_Y = center.y - perpY * flameWidth * 0.25f

        reusablePath.reset()
        reusablePath.moveTo(innerBaseL_X, innerBaseL_Y)
        reusablePath.lineTo(innerTipX, innerTipY)
        reusablePath.lineTo(innerBaseR_X, innerBaseR_Y)
        reusablePath.close()
        canvas.drawPath(reusablePath, fillPaint)
    }

    /**
     * Draws a sci-fi HUD panel with dark translucent background and neon borders.
     */
    fun drawTechPanel(
        canvas: Canvas,
        rect: RectF,
        bgColor: Int = 0xCC0D1B2A.toInt(),
        borderColor: Int = 0xFF00E5FF.toInt(),
        cornerRadius: Float = 12f
    ) {
        fillPaint.color = bgColor
        fillPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint)

        strokePaint.color = borderColor
        strokePaint.strokeWidth = 2f
        strokePaint.alpha = 180
        strokePaint.style = Paint.Style.STROKE
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, strokePaint)
    }

    /**
     * Draws a stat progress bar with background container and filled gradient bar.
     */
    fun drawStatBar(
        canvas: Canvas,
        rect: RectF,
        fraction: Float,
        fillColor: Int,
        bgColor: Int = 0xFF1B263B.toInt()
    ) {
        fillPaint.color = bgColor
        fillPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, fillPaint)

        val fillW = (rect.width() * fraction.coerceIn(0f, 1f)).coerceAtLeast(rect.height())
        val fillRect = RectF(rect.left, rect.top, rect.left + fillW, rect.bottom)
        fillPaint.color = fillColor
        canvas.drawRoundRect(fillRect, rect.height() / 2f, rect.height() / 2f, fillPaint)
    }

    // ---- Modern glass UI kit ----

    private val glassFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val glassStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val uiText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isFakeBoldText = true
    }
    private val tmpRect = RectF()

    /** Frosted glass card: vertical gradient, accent border, top highlight and soft drop shadow. */
    fun drawGlassPanel(
        canvas: Canvas,
        rect: RectF,
        accent: Int,
        radius: Float = 18f,
        topColor: Int = 0xE61B2A49.toInt(),
        bottomColor: Int = 0xE60A1226.toInt()
    ) {
        // soft shadow
        glowPaint.shader = null
        glowPaint.color = 0x55000000
        tmpRect.set(rect.left, rect.top + 4f, rect.right, rect.bottom + 4f)
        canvas.drawRoundRect(tmpRect, radius, radius, glowPaint)

        glassFill.shader = android.graphics.LinearGradient(
            0f, rect.top, 0f, rect.bottom, topColor, bottomColor, android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, radius, radius, glassFill)
        glassFill.shader = null

        // top gloss
        glassFill.color = 0x22FFFFFF
        tmpRect.set(rect.left + 3f, rect.top + 3f, rect.right - 3f, rect.top + rect.height() * 0.45f)
        canvas.drawRoundRect(tmpRect, radius * 0.8f, radius * 0.8f, glassFill)

        glassStroke.strokeWidth = 2.2f
        glassStroke.color = accent
        glassStroke.alpha = 200
        canvas.drawRoundRect(rect, radius, radius, glassStroke)
    }

    /** Rounded progress bar with gradient fill and a bright cap. */
    fun drawProgressPill(canvas: Canvas, rect: RectF, fraction: Float, color: Int) {
        val r = rect.height() / 2f
        glassFill.shader = null
        glassFill.color = 0x66000000
        canvas.drawRoundRect(rect, r, r, glassFill)
        val w = (rect.width() * fraction.coerceIn(0f, 1f)).coerceAtLeast(if (fraction > 0f) rect.height() else 0f)
        if (w <= 0f) return
        tmpRect.set(rect.left, rect.top, rect.left + w, rect.bottom)
        val light = android.graphics.Color.argb(
            255,
            (android.graphics.Color.red(color) + 70).coerceAtMost(255),
            (android.graphics.Color.green(color) + 70).coerceAtMost(255),
            (android.graphics.Color.blue(color) + 70).coerceAtMost(255)
        )
        glassFill.shader = android.graphics.LinearGradient(
            tmpRect.left, 0f, tmpRect.right, 0f, color, light, android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(tmpRect, r, r, glassFill)
        glassFill.shader = null
    }

    /** Text with a soft drop shadow, for readable HUD numbers over any theme. */
    fun drawShadowText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        align: Paint.Align = Paint.Align.LEFT,
        alpha: Int = 255
    ) {
        uiText.textSize = size
        uiText.textAlign = align
        uiText.setShadowLayer(size * 0.12f, 0f, size * 0.06f, 0xAA000000.toInt())
        uiText.color = color
        uiText.alpha = alpha
        canvas.drawText(text, x, y, uiText)
        uiText.clearShadowLayer()
    }

    /** Glossy pill button with gradient, shine and bottom edge for a tactile feel. */
    fun drawGlossyButton(
        canvas: Canvas,
        rect: RectF,
        text: String,
        color: Int,
        textColor: Int
    ) {
        val r = rect.height() / 2f
        val dark = android.graphics.Color.rgb(
            (android.graphics.Color.red(color) * 0.62f).toInt(),
            (android.graphics.Color.green(color) * 0.62f).toInt(),
            (android.graphics.Color.blue(color) * 0.62f).toInt()
        )
        // bottom edge (3D press look)
        glassFill.shader = null
        glassFill.color = dark
        tmpRect.set(rect.left, rect.top + 5f, rect.right, rect.bottom + 5f)
        canvas.drawRoundRect(tmpRect, r, r, glassFill)

        val light = android.graphics.Color.rgb(
            (android.graphics.Color.red(color) + 50).coerceAtMost(255),
            (android.graphics.Color.green(color) + 50).coerceAtMost(255),
            (android.graphics.Color.blue(color) + 50).coerceAtMost(255)
        )
        glassFill.shader = android.graphics.LinearGradient(
            0f, rect.top, 0f, rect.bottom, light, color, android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, r, r, glassFill)
        glassFill.shader = null

        glassFill.color = 0x38FFFFFF
        tmpRect.set(rect.left + r * 0.5f, rect.top + 3f, rect.right - r * 0.5f, rect.top + rect.height() * 0.45f)
        canvas.drawRoundRect(tmpRect, r * 0.6f, r * 0.6f, glassFill)

        uiText.textSize = (rect.height() * 0.42f).coerceAtMost(60f)
        uiText.textAlign = Paint.Align.CENTER
        uiText.color = textColor
        uiText.alpha = 255
        // shrink to fit
        val maxW = rect.width() * 0.92f
        val tw = uiText.measureText(text)
        if (tw > maxW) uiText.textSize = uiText.textSize * maxW / tw
        val ty = rect.centerY() - (uiText.descent() + uiText.ascent()) / 2f
        canvas.drawText(text, rect.centerX(), ty, uiText)
    }

    /**
     * Draws a glowing rotating gravitational or magnetic orbit ring.
     */
    fun drawOrbitRing(
        canvas: Canvas,
        center: Vector2,
        radius: Float,
        timer: Float,
        color: Int = 0xFFFFD700.toInt()
    ) {
        val pulse = 1f + 0.04f * sin(timer * 5.0).toFloat()
        val curRadius = radius * pulse
        strokePaint.color = color
        strokePaint.strokeWidth = 2.5f
        strokePaint.alpha = (150 + 70 * sin(timer * 6.0)).toInt().coerceIn(0, 255)
        strokePaint.style = Paint.Style.STROKE
        canvas.drawCircle(center.x, center.y, curRadius, strokePaint)

        // Orbit sparkles
        val sparkleCount = 4
        val step = (Math.PI * 2.0 / sparkleCount).toFloat()
        fillPaint.color = color
        fillPaint.style = Paint.Style.FILL
        for (i in 0 until sparkleCount) {
            val a = timer * 2.5f + i * step
            val sx = center.x + cos(a) * curRadius
            val sy = center.y + sin(a) * curRadius
            canvas.drawCircle(sx, sy, 4.5f, fillPaint)
        }
    }
}

