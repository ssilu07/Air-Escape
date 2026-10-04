package com.royals.airescape.screen

import android.app.AlertDialog
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.widget.EditText
import android.widget.LinearLayout
import com.royals.airescape.ads.AdManager
import com.royals.airescape.data.Constants
import com.royals.airescape.data.GameData
import com.royals.airescape.engine.GameSurfaceView
import com.royals.airescape.engine.Renderer
import com.royals.airescape.util.Vector2
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Earning Wallet & Withdrawal screen.
 * Allows users to view their coin balance, cash equivalent (1 Star = 1 Coin),
 * enter their UPI ID, and submit withdrawal requests.
 */
class WalletScreen(private val surfaceView: GameSurfaceView) : Screen {

    // ── Layout state ─────────────────────────────────────────────────
    private var screenWidth = 0f
    private var screenHeight = 0f
    private var layoutDone = false

    // ── Button and interactive rects ─────────────────────────────────
    private val backButtonRect = RectF()
    private val upiInputRect = RectF()
    private val withdrawButtonRect = RectF()

    // Quick withdrawal amount options (in Rupees: ₹100, ₹150, ₹200, ₹250)
    private val amountOptions = listOf(100, 150, 200, 250)
    private val amountRects = mutableListOf<RectF>()
    private var selectedCoinsIndex = 0

    // ── User state ───────────────────────────────────────────────────
    private var currentUpiId: String = ""
    private var statusMessage: String? = null
    private var statusIsError: Boolean = false

    // ── Paints ───────────────────────────────────────────────────────
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF16213E.toInt()
        style = Paint.Style.FILL
    }

    private val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFD700.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val subCardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0F3460.toInt()
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // ── Init ─────────────────────────────────────────────────────────

    override fun onEnter() {
        currentUpiId = GameData.savedUpiId
        AdManager.showBanner()
        // Refresh wallet and transaction status from Supabase
        GameData.syncWithCloud {
            currentUpiId = GameData.savedUpiId
        }
    }

    override fun onExit() {
        AdManager.hideBanner()
    }

    override fun onBackPressed(): Boolean {
        surfaceView.setScreen(MenuScreen(surfaceView))
        return true
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

        val padding = sw * 0.05f

        // 1. Back button (safely below status bar & cutout)
        val backBtnW = sw * 0.24f
        val backBtnH = (usableH * 0.052f).coerceIn(70f, 130f)
        val topY = safeTop + usableH * 0.015f
        backButtonRect.set(padding, topY, padding + backBtnW, topY + backBtnH)

        // 2. UPI input field rect
        val balanceCardTop = topY + backBtnH + usableH * 0.045f
        val balanceCardH = usableH * 0.21f

        val upiTop = balanceCardTop + balanceCardH + usableH * 0.035f
        val upiH = usableH * 0.075f
        upiInputRect.set(padding, upiTop, sw - padding, upiTop + upiH)

        // 3. Amount chips (row of 4)
        amountRects.clear()
        val chipsTop = upiTop + upiH + usableH * 0.042f
        val chipGap = sw * 0.02f
        val chipW = (sw - padding * 2f - chipGap * 3f) / 4f
        val chipH = usableH * 0.072f

        for (i in amountOptions.indices) {
            val left = padding + i * (chipW + chipGap)
            amountRects.add(RectF(left, chipsTop, left + chipW, chipsTop + chipH))
        }

        // 4. Withdraw button
        val withdrawTop = chipsTop + chipH + usableH * 0.028f
        val withdrawH = usableH * 0.075f
        withdrawButtonRect.set(padding, withdrawTop, sw - padding, withdrawTop + withdrawH)

        layoutDone = true
    }

    // ── Render ───────────────────────────────────────────────────────

    override fun update(dt: Float) {
        if (!layoutDone) layoutIfNeeded()
    }

    override fun render(canvas: Canvas) {
        if (!layoutDone) return

        // Dark background
        canvas.drawColor(Constants.BACKGROUND_COLOR.toInt())

        val padding = screenWidth * 0.05f
        val cx = screenWidth / 2f
        val safeTop = GameSurfaceView.safeInsetTop
        val safeBottom = GameSurfaceView.safeInsetBottom + GameSurfaceView.bannerHeight
        val usableH = (screenHeight - safeTop - safeBottom).coerceAtLeast(screenHeight * 0.70f)

        // 1. Top bar: Back Button & Title
        Renderer.drawButton(
            canvas, backButtonRect, "< MENU",
            color = 0xFF78909C, textColor = 0xFFFFFFFF
        )

        titlePaint.textSize = screenWidth * 0.065f
        titlePaint.color = 0xFFFFFFFF.toInt()
        canvas.drawText("EARNING WALLET", cx, backButtonRect.centerY() + titlePaint.textSize * 0.35f, titlePaint)

        // Player ID & Supabase Cloud Status
        textPaint.color = 0xFF80D8FF.toInt()
        textPaint.textSize = screenWidth * 0.026f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.isFakeBoldText = false
        val syncStatus = if (com.royals.airescape.data.SupabaseClient.isSyncing) "☁️ Syncing..." else "☁️ Supabase Cloud"
        canvas.drawText("ID: ${GameData.playerId}  •  $syncStatus", cx, backButtonRect.bottom + usableH * 0.022f, textPaint)

        // 2. Balance Card (top section)
        val balanceCardTop = backButtonRect.bottom + usableH * 0.045f
        val balanceCardH = usableH * 0.21f
        val balanceRect = RectF(padding, balanceCardTop, screenWidth - padding, balanceCardTop + balanceCardH)

        canvas.drawRoundRect(balanceRect, 20f, 20f, cardBgPaint)
        canvas.drawRoundRect(balanceRect, 20f, 20f, cardBorderPaint)

        // Card header
        textPaint.color = 0xFFAAAAAA.toInt()
        textPaint.textSize = screenWidth * 0.032f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.isFakeBoldText = false
        canvas.drawText("TOTAL COIN BALANCE", cx, balanceCardTop + balanceCardH * 0.22f, textPaint)

        // Big Coin Count
        textPaint.color = Constants.COIN_COLOR.toInt()
        textPaint.textSize = screenWidth * 0.082f
        textPaint.isFakeBoldText = true
        val coinStr = "\uD83E\uDE99 ${GameData.totalCoins} COINS"
        canvas.drawText(coinStr, cx, balanceCardTop + balanceCardH * 0.52f, textPaint)

        // Cash Value
        val rupeeVal = GameData.totalCoins.toFloat() / Constants.COINS_PER_RUPEE.toFloat()
        textPaint.color = 0xFF00E676.toInt()
        textPaint.textSize = screenWidth * 0.046f
        val cashStr = "= \u20B9${String.format(Locale.US, "%.2f", rupeeVal)} INR"
        canvas.drawText(cashStr, cx, balanceCardTop + balanceCardH * 0.74f, textPaint)

        // Rate note
        textPaint.color = 0xFF888888.toInt()
        textPaint.textSize = screenWidth * 0.025f
        textPaint.isFakeBoldText = false
        canvas.drawText(
            "1 Star = 1 Coin  \u2022  ${Constants.COINS_PER_RUPEE} Coins = \u20B91.00  \u2022  Min Payment: \u20B9${Constants.MIN_WITHDRAW_RUPEES}",
            cx, balanceCardTop + balanceCardH * 0.90f, textPaint
        )

        // 3. Section: Payout UPI ID
        textPaint.color = 0xFFFFFFFF.toInt()
        textPaint.textSize = screenWidth * 0.034f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.isFakeBoldText = true
        canvas.drawText("WITHDRAWAL UPI ID / PAYTM", padding, upiInputRect.top - usableH * 0.012f, textPaint)

        canvas.drawRoundRect(upiInputRect, 14f, 14f, subCardBgPaint)

        val upiText = if (currentUpiId.isNotBlank()) currentUpiId else "Tap to enter UPI ID (e.g. 9876543210@paytm)"
        textPaint.color = if (currentUpiId.isNotBlank()) 0xFFFFFFFF.toInt() else 0xFF888888.toInt()
        textPaint.textSize = screenWidth * 0.036f
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(
            upiText,
            upiInputRect.left + screenWidth * 0.035f,
            upiInputRect.centerY() + textPaint.textSize * 0.35f,
            textPaint
        )

        // Small Edit Tag on right
        val editTag = "EDIT"
        textPaint.color = 0xFF40C4FF.toInt()
        textPaint.textSize = screenWidth * 0.032f
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.isFakeBoldText = true
        canvas.drawText(
            editTag,
            upiInputRect.right - screenWidth * 0.035f,
            upiInputRect.centerY() + textPaint.textSize * 0.35f,
            textPaint
        )

        // 4. Amount Selection Chips
        textPaint.color = 0xFFFFFFFF.toInt()
        textPaint.textSize = screenWidth * 0.034f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.isFakeBoldText = true
        val chipsLabelY = amountRects[0].top - screenHeight * 0.01f
        canvas.drawText("SELECT WITHDRAWAL AMOUNT", padding, chipsLabelY, textPaint)

        for (i in amountOptions.indices) {
            val rect = amountRects[i]
            val isSelected = (i == selectedCoinsIndex)
            val rs = amountOptions[i]
            val coins = rs * Constants.COINS_PER_RUPEE

            if (isSelected) {
                cardBgPaint.color = 0xFF00E676.toInt()
                cardBgPaint.alpha = 60
                cardBorderPaint.color = 0xFF00E676.toInt()
            } else {
                cardBgPaint.color = 0xFF16213E.toInt()
                cardBgPaint.alpha = 255
                cardBorderPaint.color = 0xFF3E4A61.toInt()
            }

            canvas.drawRoundRect(rect, 12f, 12f, cardBgPaint)
            canvas.drawRoundRect(rect, 12f, 12f, cardBorderPaint)

            // Text on chip
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.isFakeBoldText = true
            textPaint.textSize = screenWidth * 0.033f
            textPaint.color = if (isSelected) 0xFF00E676.toInt() else 0xFFFFFFFF.toInt()
            canvas.drawText("\u20B9$rs", rect.centerX(), rect.centerY() - rect.height() * 0.08f, textPaint)

            textPaint.textSize = screenWidth * 0.023f
            textPaint.color = if (isSelected) 0xFFB9F6CA.toInt() else 0xFFAAAAAA.toInt()
            textPaint.isFakeBoldText = false
            val coinFmt = java.text.NumberFormat.getIntegerInstance(Locale.US).format(coins)
            canvas.drawText("${coinFmt}\uD83E\uDE99", rect.centerX(), rect.centerY() + rect.height() * 0.32f, textPaint)
        }

        // 5. Withdraw Button
        val selectedRs = amountOptions[selectedCoinsIndex]
        val selectedCoins = selectedRs * Constants.COINS_PER_RUPEE
        val canWithdraw = GameData.totalCoins >= selectedCoins && currentUpiId.isNotBlank()

        val btnColor = if (canWithdraw) 0xFF00E676 else 0xFF455A64
        val btnTextColor = if (canWithdraw) 0xFF1A1A2E else 0xFF90A4AE
        Renderer.drawButton(
            canvas, withdrawButtonRect,
            "WITHDRAW \u20B9$selectedRs TO UPI",
            color = btnColor,
            textColor = btnTextColor
        )

        // 6. Status Message (if any)
        var contentY = withdrawButtonRect.bottom + screenHeight * 0.03f
        if (statusMessage != null) {
            textPaint.color = if (statusIsError) 0xFFFF5252.toInt() else 0xFF00E676.toInt()
            textPaint.textSize = screenWidth * 0.030f
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.isFakeBoldText = true
            canvas.drawText(statusMessage!!, cx, contentY, textPaint)
            contentY += screenHeight * 0.028f
        }

        // 7. Recent Transactions List
        renderTransactionHistory(canvas, padding, contentY)
    }

    private fun renderTransactionHistory(canvas: Canvas, padding: Float, startY: Float) {
        val safeBottom = GameSurfaceView.safeInsetBottom + GameSurfaceView.bannerHeight
        val maxAllowedBottom = screenHeight - safeBottom - screenHeight * 0.015f
        if (startY >= maxAllowedBottom - screenHeight * 0.04f) return

        val history = GameData.getWithdrawalHistory()
        val cx = screenWidth / 2f

        textPaint.color = 0xFFCCCCCC.toInt()
        textPaint.textSize = screenWidth * 0.033f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.isFakeBoldText = true
        canvas.drawText("RECENT WITHDRAWALS", padding, startY, textPaint)

        var rowY = startY + screenHeight * 0.022f

        if (history.isEmpty()) {
            textPaint.color = 0xFF777777.toInt()
            textPaint.textSize = screenWidth * 0.029f
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.isFakeBoldText = false
            canvas.drawText("No withdrawal requests yet. Collect stars to earn!", cx, rowY + screenHeight * 0.025f, textPaint)
            return
        }

        val df = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

        for (req in history.take(3)) {
            val rowH = screenHeight * 0.048f
            if (rowY + rowH > maxAllowedBottom) break

            val rowRect = RectF(padding, rowY, screenWidth - padding, rowY + rowH)
            canvas.drawRoundRect(rowRect, 10f, 10f, subCardBgPaint)

            // Amount & UPI ID
            textPaint.color = 0xFFFFFFFF.toInt()
            textPaint.textSize = screenWidth * 0.031f
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.isFakeBoldText = true
            val dateStr = df.format(Date(req.timestamp))
            canvas.drawText(
                "\u20B9${String.format(Locale.US, "%.2f", req.rupeeAmount)} \u2192 ${req.upiId}",
                rowRect.left + screenWidth * 0.03f,
                rowRect.centerY() - rowRect.height() * 0.06f,
                textPaint
            )

            textPaint.color = 0xFF888888.toInt()
            textPaint.textSize = screenWidth * 0.023f
            textPaint.isFakeBoldText = false
            canvas.drawText(
                dateStr,
                rowRect.left + screenWidth * 0.03f,
                rowRect.centerY() + rowRect.height() * 0.32f,
                textPaint
            )

            // Status Pill
            val isPending = req.status == "PENDING"
            val pillColor = if (isPending) 0xFFFFD740.toInt() else 0xFF00E676.toInt()
            textPaint.color = pillColor
            textPaint.textSize = screenWidth * 0.027f
            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.isFakeBoldText = true
            canvas.drawText(
                req.status,
                rowRect.right - screenWidth * 0.03f,
                rowRect.centerY() + textPaint.textSize * 0.35f,
                textPaint
            )

            rowY += rowH + screenHeight * 0.010f
        }
    }

    // ── Input Dialog ─────────────────────────────────────────────────

    private fun showUpiInputDialog() {
        val act = surfaceView.context
        val input = EditText(act).apply {
            hint = "yourname@upi or 9876543210@paytm"
            setText(currentUpiId)
            setSingleLine(true)
        }

        val container = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (act.resources.displayMetrics.density * 20).toInt()
            setPadding(pad, pad / 2, pad, pad / 2)
            addView(input)
        }

        AlertDialog.Builder(act)
            .setTitle("Enter UPI / Paytm ID")
            .setMessage("Withdrawal payment will be sent directly to this address:")
            .setView(container)
            .setPositiveButton("SAVE") { _, _ ->
                val upi = input.text.toString().trim()
                currentUpiId = upi
                GameData.savedUpiId = upi
                statusMessage = "UPI ID saved!"
                statusIsError = false
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun confirmAndWithdraw() {
        val rupee = amountOptions[selectedCoinsIndex]
        val coins = rupee * Constants.COINS_PER_RUPEE

        val act = surfaceView.context
        val coinFmt = java.text.NumberFormat.getIntegerInstance(Locale.US).format(coins)
        AlertDialog.Builder(act)
            .setTitle("Confirm Withdrawal")
            .setMessage("Withdraw \u20B9$rupee ($coinFmt Coins) to UPI ID:\n\n$currentUpiId\n\nProcess withdrawal request?")
            .setPositiveButton("CONFIRM") { _, _ ->
                val result = GameData.submitWithdrawal(currentUpiId, coins)
                statusMessage = result.second
                statusIsError = !result.first

                if (result.first) {
                    AlertDialog.Builder(act)
                        .setTitle("Request Received! \u2705")
                        .setMessage("Your request for \u20B9$rupee has been queued.\n\nTransaction ID: ${GameData.getWithdrawalHistory().firstOrNull()?.id ?: ""}\nPayment will be credited to $currentUpiId within 24-48 hours.")
                        .setPositiveButton("GREAT", null)
                        .show()
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    // ── Touch Handling ───────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && layoutDone) {
            val x = event.x
            val y = event.y

            // Back button
            if (backButtonRect.contains(x, y)) {
                surfaceView.setScreen(MenuScreen(surfaceView))
                return true
            }

            // UPI ID input rect
            if (upiInputRect.contains(x, y)) {
                surfaceView.post { showUpiInputDialog() }
                return true
            }

            // Amount chips
            for (i in amountRects.indices) {
                if (amountRects[i].contains(x, y)) {
                    selectedCoinsIndex = i
                    return true
                }
            }

            // Withdraw button
            if (withdrawButtonRect.contains(x, y)) {
                val rupee = amountOptions[selectedCoinsIndex]
                val coins = rupee * Constants.COINS_PER_RUPEE
                if (currentUpiId.isBlank() || !currentUpiId.contains("@")) {
                    surfaceView.post { showUpiInputDialog() }
                    return true
                }
                if (GameData.totalCoins < coins) {
                    val coinFmt = java.text.NumberFormat.getIntegerInstance(Locale.US).format(coins)
                    statusMessage = "Insufficient coins! You need $coinFmt coins for \u20B9$rupee."
                    statusIsError = true
                    return true
                }
                surfaceView.post { confirmAndWithdraw() }
                return true
            }
        }
        return true
    }
}
