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
}
