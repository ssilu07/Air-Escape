package com.royals.airescape.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists game data (high score, settings, unlocks) using SharedPreferences.
 * Call [init] once from the Activity before any other access.
 */
object GameData {

    private const val PREFS_NAME = "missiles_game"

    private lateinit var prefs: SharedPreferences

    val playerId: String
        get() {
            var id = prefs.getString("player_id", null)
            if (id.isNullOrBlank()) {
                id = "usr_" + java.util.UUID.randomUUID().toString().replace("-", "").take(12)
                prefs.edit().putString("player_id", id).apply()
            }
            return id
        }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Ensure player account exists
        val currentId = playerId

        // Sync with Supabase Cloud in background
        syncWithCloud()
    }

    fun syncWithCloud(onComplete: ((Boolean) -> Unit)? = null) {
        val currentId = playerId
        SupabaseClient.fetchWallet(currentId) { remoteCoins, remoteStars, remoteUpi ->
            if (remoteCoins != null) {
                // If cloud has higher balance (e.g. cloud reward/admin credit), sync to local
                if (remoteCoins > totalCoins) {
                    totalCoins = remoteCoins
                } else if (totalCoins > remoteCoins) {
                    // Local has more earned offline, sync up to Supabase
                    SupabaseClient.syncWallet(currentId, totalCoins, totalStars, savedUpiId)
                }
                if (remoteStars != null && remoteStars > totalStars) {
                    totalStars = remoteStars
                }
                if (!remoteUpi.isNullOrBlank() && savedUpiId.isBlank()) {
                    savedUpiId = remoteUpi
                }
            } else {
                // New user registration on Supabase
                SupabaseClient.syncWallet(currentId, totalCoins, totalStars, savedUpiId)
            }
            onComplete?.invoke(true)
        }

        // Also fetch latest withdrawal statuses (e.g. admin marked as PAID)
        SupabaseClient.fetchWithdrawals(currentId) { remoteList ->
            if (remoteList != null && remoteList.isNotEmpty()) {
                saveWithdrawalHistory(remoteList)
            }
        }
    }

    var totalStars: Int
        get() = prefs.getInt("total_stars", 0)
        set(value) = prefs.edit().putInt("total_stars", value).apply()

    // ── Coins & Earning Wallet ────────────────────────────────────────
    var totalCoins: Int
        get() {
            if (!prefs.contains("total_coins")) {
                val initialCoins = totalStars // Existing stars convert 1:1 to coins
                prefs.edit().putInt("total_coins", initialCoins).apply()
                return initialCoins
            }
            return prefs.getInt("total_coins", 0)
        }
        set(value) = prefs.edit().putInt("total_coins", value.coerceAtLeast(0)).apply()

    var savedUpiId: String
        get() = prefs.getString("saved_upi_id", "") ?: ""
        set(value) = prefs.edit().putString("saved_upi_id", value.trim()).apply()

    fun addCoins(amount: Int) {
        if (amount <= 0) return
        totalCoins += amount
        SupabaseClient.syncWallet(playerId, totalCoins, totalStars, savedUpiId)
    }

    fun deductCoins(amount: Int): Boolean {
        if (amount <= 0 || totalCoins < amount) return false
        totalCoins -= amount
        SupabaseClient.syncWallet(playerId, totalCoins, totalStars, savedUpiId)
        return true
    }

    var highScore: Int
        get() = prefs.getInt("high_score", 0)
        set(value) = prefs.edit().putInt("high_score", value).apply()


    val unlockedPlanes: MutableSet<Int>
        get() = prefs.getStringSet("unlocked_planes", setOf("0"))!!.map { it.toInt() }.toMutableSet()

    fun unlockPlane(id: Int) {
        val current = prefs.getStringSet("unlocked_planes", setOf("0"))!!.toMutableSet()
        current.add(id.toString())
        prefs.edit().putStringSet("unlocked_planes", current).apply()
    }

    fun isPlaneUnlocked(id: Int): Boolean = unlockedPlanes.contains(id)

    var selectedPlane: Int
        get() = prefs.getInt("selected_plane", 0)
        set(value) = prefs.edit().putInt("selected_plane", value).apply()

    var controlType: Int
        get() = prefs.getInt("control_type", 0) // 0=joystick, 1=touch, 2=buttons
        set(value) = prefs.edit().putInt("control_type", value).apply()

    var hardMode: Boolean
        get() = prefs.getBoolean("hard_mode", false)
        set(value) = prefs.edit().putBoolean("hard_mode", value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()

    var tutorialShown: Boolean
        get() = prefs.getBoolean("tutorial_shown", false)
        set(value) = prefs.edit().putBoolean("tutorial_shown", value).apply()

    var colorblindMode: Boolean
        get() = prefs.getBoolean("colorblind_mode", false)
        set(value) = prefs.edit().putBoolean("colorblind_mode", value).apply()

    // ── Theme system ──────────────────────────────────────────────────

    var selectedTheme: Int
        get() = prefs.getInt("selected_theme", 0)
        set(value) = prefs.edit().putInt("selected_theme", value).apply()

    val unlockedThemes: MutableSet<Int>
        get() = prefs.getStringSet("unlocked_themes", setOf("0"))!!.map { it.toInt() }.toMutableSet()

    fun unlockTheme(id: Int) {
        val current = prefs.getStringSet("unlocked_themes", setOf("0"))!!.toMutableSet()
        current.add(id.toString())
        prefs.edit().putStringSet("unlocked_themes", current).apply()
    }

    fun isThemeUnlocked(id: Int): Boolean = unlockedThemes.contains(id)

    /**
     * Submit a score after a game session. Updates high score if beaten
     * and accumulates stars collected and coins earned (1 Star = 1 Coin).
     */
    fun submitScore(score: Int, starsCollected: Int) {
        if (score > highScore) {
            highScore = score
        }
        totalStars += starsCollected
        addCoins(starsCollected * Constants.COINS_PER_STAR)
    }

    // ── Withdrawal requests ──────────────────────────────────────────
    data class WithdrawalRequest(
        val id: String,
        val upiId: String,
        val coins: Int,
        val rupeeAmount: Float,
        val timestamp: Long,
        var status: String = "PENDING"
    )

    fun getWithdrawalHistory(): List<WithdrawalRequest> {
        val raw = prefs.getString("withdrawal_history", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 5) {
                WithdrawalRequest(
                    id = parts[0],
                    upiId = parts[1],
                    coins = parts[2].toIntOrNull() ?: 0,
                    rupeeAmount = parts[3].toFloatOrNull() ?: 0f,
                    timestamp = parts[4].toLongOrNull() ?: 0L,
                    status = if (parts.size > 5) parts[5] else "PENDING"
                )
            } else null
        }
    }

    fun saveWithdrawalHistory(list: List<WithdrawalRequest>) {
        val raw = list.take(20).joinToString(";") {
            "${it.id}|${it.upiId}|${it.coins}|${it.rupeeAmount}|${it.timestamp}|${it.status}"
        }
        prefs.edit().putString("withdrawal_history", raw).apply()
    }

    fun submitWithdrawal(upiId: String, coins: Int): Pair<Boolean, String> {
        val cleanUpi = upiId.trim()
        if (cleanUpi.length < 5 || !cleanUpi.contains("@")) {
            return Pair(false, "Please enter a valid UPI ID (e.g. mobile@upi)")
        }
        if (coins < Constants.MIN_WITHDRAW_COINS) {
            val minRs = Constants.MIN_WITHDRAW_COINS / Constants.COINS_PER_RUPEE
            return Pair(false, "Minimum withdrawal is ${Constants.MIN_WITHDRAW_COINS} coins (₹$minRs)")
        }
        if (totalCoins < coins) {
            return Pair(false, "Insufficient coin balance!")
        }

        if (deductCoins(coins)) {
            savedUpiId = cleanUpi
            val rupee = coins.toFloat() / Constants.COINS_PER_RUPEE.toFloat()
            val newTxn = WithdrawalRequest(
                id = "TXN" + (System.currentTimeMillis() % 1000000),
                upiId = cleanUpi,
                coins = coins,
                rupeeAmount = rupee,
                timestamp = System.currentTimeMillis(),
                status = "PENDING"
            )
            val current = getWithdrawalHistory().toMutableList()
            current.add(0, newTxn)
            saveWithdrawalHistory(current)

            // Submit withdrawal to Supabase cloud table
            SupabaseClient.submitWithdrawal(newTxn, playerId)
            SupabaseClient.syncWallet(playerId, totalCoins, totalStars, cleanUpi)

            return Pair(true, "Request of ₹${String.format(java.util.Locale.US, "%.2f", rupee)} submitted successfully! Processing to $cleanUpi.")
        }
        return Pair(false, "Could not process withdrawal. Please try again.")
    }


    // ── Daily challenges ─────────────────────────────────────────────

    var dailyChallengeDay: Int
        get() = prefs.getInt("daily_challenge_day", 0)
        set(value) = prefs.edit().putInt("daily_challenge_day", value).apply()

    fun saveChallenges(challenges: List<DailyChallenge>) {
        val editor = prefs.edit()
        for ((i, c) in challenges.withIndex()) {
            editor.putString("challenge_${i}_type", c.type.name)
            editor.putInt("challenge_${i}_target", c.target)
            editor.putInt("challenge_${i}_progress", c.progress)
        }
        editor.putInt("challenge_count", challenges.size)
        editor.apply()
    }

    fun loadChallenges(): List<DailyChallenge> {
        val count = prefs.getInt("challenge_count", 0)
        val list = mutableListOf<DailyChallenge>()
        for (i in 0 until count) {
            val typeName = prefs.getString("challenge_${i}_type", null) ?: continue
            val type = try { ChallengeType.valueOf(typeName) } catch (_: Exception) { continue }
            val target = prefs.getInt("challenge_${i}_target", 10)
            val progress = prefs.getInt("challenge_${i}_progress", 0)
            list.add(DailyChallenge(type, target, progress))
        }
        return list
    }

    fun isChallengeRewarded(challenge: DailyChallenge): Boolean {
        val key = "challenge_rewarded_${challenge.type.name}_${challenge.target}"
        return prefs.getBoolean(key, false)
    }

    fun markChallengeRewarded(challenge: DailyChallenge) {
        val key = "challenge_rewarded_${challenge.type.name}_${challenge.target}"
        prefs.edit().putBoolean(key, true).apply()
    }
}
