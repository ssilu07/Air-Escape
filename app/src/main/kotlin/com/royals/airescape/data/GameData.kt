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

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var totalStars: Int
        get() = prefs.getInt("total_stars", 0)
        set(value) = prefs.edit().putInt("total_stars", value).apply()

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
     * and accumulates stars collected.
     */
    fun submitScore(score: Int, starsCollected: Int) {
        if (score > highScore) {
            highScore = score
        }
        totalStars += starsCollected
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
