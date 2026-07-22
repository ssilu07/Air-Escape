package com.royals.airescape.screen

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.royals.airescape.data.Constants
import com.royals.airescape.data.GameData
import com.royals.airescape.engine.GameSurfaceView
import com.royals.airescape.engine.Renderer
import com.royals.airescape.engine.SoundManager

/**
 * Settings screen.
 *
 * Allows the player to configure:
 * - Control scheme: Joystick / Touch / Buttons
 * - Game mode: Normal / Fast
 * - Sound: On / Off
 *
 * All settings are persisted via [GameData].
 */
class SettingsScreen(private val surfaceView: GameSurfaceView) : Screen {

    // ── Layout ───────────────────────────────────────────────────────
    private var screenWidth = 0f
    private var screenHeight = 0f
    private var layoutDone = false

    // ── Button regions ───────────────────────────────────────────────
    // Control scheme radio buttons
    private val joystickRect = RectF()
    private val touchRect = RectF()
    private val buttonsRect = RectF()

    // Game mode toggle
    private val normalModeRect = RectF()
    private val hardModeRect = RectF()

    // Sound toggle
    private val soundOnRect = RectF()
    private val soundOffRect = RectF()

    // Colorblind toggle
    private val cbOnRect = RectF()
    private val cbOffRect = RectF()

    // Cloud sync button
    private val cloudSyncRect = RectF()

    // Back button
    private val backButtonRect = RectF()

    // ── Paints ───────────────────────────────────────────────────────
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFCCCCCC.toInt()
        isFakeBoldText = true
        textAlign = Paint.Align.LEFT
    }

    private val optionBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val optionTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val selectedBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00E676.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    // ── Layout ───────────────────────────────────────────────────────

    private fun layoutIfNeeded() {
        val sw = GameSurfaceView.screenWidth.toFloat()
        val sh = GameSurfaceView.screenHeight.toFloat()
        if (sw <= 0f || sh <= 0f) return

        screenWidth = sw
        screenHeight = sh

        val padding = sw * 0.05f
        val optionW = (sw - padding * 4f) / 3f
        val optionH = sh * 0.05f

        // ── Control Scheme (3 radio buttons in a row) ────────────────
        val controlY = sh * 0.18f
        joystickRect.set(padding, controlY, padding + optionW, controlY + optionH)
        touchRect.set(padding * 2f + optionW, controlY, padding * 2f + optionW * 2f, controlY + optionH)
        buttonsRect.set(padding * 3f + optionW * 2f, controlY, padding * 3f + optionW * 3f, controlY + optionH)

        // ── Game Mode (2 buttons in a row) ───────────────────────────
        val modeY = sh * 0.34f
        val modeW = (sw - padding * 3f) / 2f
        normalModeRect.set(padding, modeY, padding + modeW, modeY + optionH)
        hardModeRect.set(padding * 2f + modeW, modeY, padding * 2f + modeW * 2f, modeY + optionH)

        // ── Sound (2 buttons in a row) ───────────────────────────────
        val soundY = sh * 0.50f
        soundOnRect.set(padding, soundY, padding + modeW, soundY + optionH)
        soundOffRect.set(padding * 2f + modeW, soundY, padding * 2f + modeW * 2f, soundY + optionH)

        // ── Colorblind mode (2 buttons in a row) ────────────────────
        val cbY = sh * 0.66f
        cbOnRect.set(padding, cbY, padding + modeW, cbY + optionH)
        cbOffRect.set(padding * 2f + modeW, cbY, padding * 2f + modeW * 2f, cbY + optionH)

        // ── Cloud sync button ──────────────────────────────────────
        val syncW = sw * 0.5f
        val syncH = sh * 0.045f
        val syncY = sh * 0.78f
        cloudSyncRect.set((sw - syncW) / 2f, syncY, (sw + syncW) / 2f, syncY + syncH)

        // ── Back button ──────────────────────────────────────────────
        val backW = sw * 0.6f
        val backH = sh * 0.055f
        backButtonRect.set(
            (sw - backW) / 2f, sh * 0.85f,
            (sw + backW) / 2f, sh * 0.85f + backH
        )

        layoutDone = true
    }

    // ── Screen interface ─────────────────────────────────────────────

    override fun update(dt: Float) {
        if (!layoutDone) layoutIfNeeded()
    }

    override fun render(canvas: Canvas) {
        if (!layoutDone) return

        canvas.drawColor(Constants.BACKGROUND_COLOR.toInt())

        val padding = screenWidth * 0.05f

        // Title
        titlePaint.textSize = screenWidth * 0.07f
        canvas.drawText("SETTINGS", screenWidth / 2f, screenHeight * 0.09f, titlePaint)

        // ── Control Scheme section ───────────────────────────────────
        sectionPaint.textSize = screenWidth * 0.04f
        canvas.drawText("CONTROL SCHEME", padding, screenHeight * 0.15f, sectionPaint)

        val controlType = GameData.controlType
        drawOption(canvas, joystickRect, "Joystick", controlType == 0)
        drawOption(canvas, touchRect, "Touch", controlType == 1)
        drawOption(canvas, buttonsRect, "Buttons", controlType == 2)

        // ── Game Mode section ────────────────────────────────────────
        canvas.drawText("GAME MODE", padding, screenHeight * 0.31f, sectionPaint)

        val hardMode = GameData.hardMode
        drawOption(canvas, normalModeRect, "Normal", !hardMode)
        drawOption(canvas, hardModeRect, "Hard", hardMode)

        // ── Sound section ────────────────────────────────────────────
        canvas.drawText("SOUND", padding, screenHeight * 0.47f, sectionPaint)

        val soundOn = GameData.soundEnabled
        drawOption(canvas, soundOnRect, "On", soundOn)
        drawOption(canvas, soundOffRect, "Off", !soundOn)

        // ── Description text ─────────────────────────────────────────
        val descPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF888888.toInt()
            textAlign = Paint.Align.CENTER
            textSize = screenWidth * 0.03f
        }

        val controlDesc = when (controlType) {
            0 -> "Drag the virtual joystick to move"
            1 -> "Touch anywhere to move toward that point"
            2 -> "Use D-pad buttons to move"
            else -> ""
        }
        canvas.drawText(controlDesc, screenWidth / 2f, screenHeight * 0.18f + screenHeight * 0.05f + screenWidth * 0.04f, descPaint)

        val modeDesc = if (hardMode) "Faster & more missiles" else "Standard game speed"
        canvas.drawText(modeDesc, screenWidth / 2f, screenHeight * 0.34f + screenHeight * 0.05f + screenWidth * 0.04f, descPaint)

        // ── Colorblind Mode section ─────────────────────────────────
        canvas.drawText("COLORBLIND MODE", padding, screenHeight * 0.63f, sectionPaint)

        val cbOn = GameData.colorblindMode
        drawOption(canvas, cbOnRect, "On", cbOn)
        drawOption(canvas, cbOffRect, "Off", !cbOn)

        val cbDesc = if (cbOn) "Symbols on missiles for clarity" else "Default missile appearance"
        canvas.drawText(cbDesc, screenWidth / 2f, screenHeight * 0.66f + screenHeight * 0.05f + screenWidth * 0.04f, descPaint)

        // ── Cloud sync button ───────────────────────────────────────
        Renderer.drawButton(
            canvas, cloudSyncRect, "CLOUD BACKUP",
            color = 0xFF40C4FF,
            textColor = 0xFFFFFFFF
        )

        // ── Back button ──────────────────────────────────────────────
        Renderer.drawButton(
            canvas, backButtonRect, "BACK",
            color = 0xFF546E7A,
            textColor = 0xFFFFFFFF
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && layoutDone) {
            val x = event.x
            val y = event.y

            // Control scheme
            if (joystickRect.contains(x, y)) {
                GameData.controlType = 0
                return true
            }
            if (touchRect.contains(x, y)) {
                GameData.controlType = 1
                return true
            }
            if (buttonsRect.contains(x, y)) {
                GameData.controlType = 2
                return true
            }

            // Game mode
            if (normalModeRect.contains(x, y)) {
                GameData.hardMode = false
                return true
            }
            if (hardModeRect.contains(x, y)) {
                GameData.hardMode = true
                return true
            }

            // Sound
            if (soundOnRect.contains(x, y)) {
                GameData.soundEnabled = true
                SoundManager.setSoundEnabled(true)
                return true
            }
            if (soundOffRect.contains(x, y)) {
                GameData.soundEnabled = false
                SoundManager.setSoundEnabled(false)
                return true
            }

            // Colorblind mode
            if (cbOnRect.contains(x, y)) {
                GameData.colorblindMode = true
                return true
            }
            if (cbOffRect.contains(x, y)) {
                GameData.colorblindMode = false
                return true
            }

            // Cloud sync — triggers Android backup manager
            if (cloudSyncRect.contains(x, y)) {
                try {
                    val bm = android.app.backup.BackupManager(surfaceView.context)
                    bm.dataChanged()
                } catch (_: Exception) { }
                return true
            }

            // Back
            if (backButtonRect.contains(x, y)) {
                surfaceView.setScreen(MenuScreen(surfaceView))
                return true
            }
        }
        return true
    }

    override fun onBackPressed(): Boolean {
        surfaceView.setScreen(MenuScreen(surfaceView))
        return true
    }

    // ── Option button rendering ──────────────────────────────────────

    private fun drawOption(canvas: Canvas, rect: RectF, label: String, selected: Boolean) {
        // Background
        if (selected) {
            optionBgPaint.color = 0xFF00E676.toInt()
            optionBgPaint.alpha = 60
        } else {
            optionBgPaint.color = 0xFF333333.toInt()
            optionBgPaint.alpha = 200
        }
        canvas.drawRoundRect(rect, 12f, 12f, optionBgPaint)

        // Selected border
        if (selected) {
            canvas.drawRoundRect(rect, 12f, 12f, selectedBorderPaint)
        }

        // Label text
        optionTextPaint.textSize = rect.height() * 0.4f
        optionTextPaint.color = if (selected) 0xFF00E676.toInt() else 0xFFCCCCCC.toInt()
        val textY = rect.centerY() - (optionTextPaint.descent() + optionTextPaint.ascent()) / 2f
        canvas.drawText(label, rect.centerX(), textY, optionTextPaint)
    }
}
