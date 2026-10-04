package com.royals.airescape.engine

import android.app.Activity
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk

object PlayGamesManager {
    private var activity: Activity? = null
    @Volatile
    var isSignedIn: Boolean = false
        private set

    const val LEADERBOARD_ID = "CgkI_replace_me"

    fun init(activity: Activity) {
        this.activity = activity
        PlayGamesSdk.initialize(activity)
        silentSignIn(activity)
    }

    private fun silentSignIn(activity: Activity) {
        PlayGames.getGamesSignInClient(activity)
            .isAuthenticated
            .addOnCompleteListener { task ->
                isSignedIn = task.isSuccessful && task.result?.isAuthenticated == true
            }
    }

    fun submitScore(score: Int) {
        val act = activity ?: return
        if (!isSignedIn) return
        try {
            PlayGames.getLeaderboardsClient(act).submitScore(LEADERBOARD_ID, score.toLong())
        } catch (_: Exception) {}
    }

    fun showLeaderboard() {
        val act = activity ?: return
        if (!isSignedIn) return
        try {
            PlayGames.getLeaderboardsClient(act)
                .getLeaderboardIntent(LEADERBOARD_ID)
                .addOnSuccessListener { intent ->
                    act.startActivityForResult(intent, 9001)
                }
        } catch (_: Exception) {}
    }

    fun release() {
        activity = null
    }
}
