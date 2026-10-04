package com.royals.airescape.data

import java.util.Calendar

enum class ChallengeType {
    COLLECT_STARS,
    SURVIVE_SECONDS,
    DESTROY_MISSILES,
    COLLECT_SHIELDS
}

data class DailyChallenge(
    val type: ChallengeType,
    val target: Int,
    var progress: Int = 0
) {
    val isCompleted: Boolean
        get() = progress >= target

    val rewardStars: Int
        get() = target / 2

    val description: String
        get() = when (type) {
            ChallengeType.COLLECT_STARS -> "Collect $target stars"
            ChallengeType.SURVIVE_SECONDS -> "Survive for $target seconds"
            ChallengeType.DESTROY_MISSILES -> "Destroy $target missiles"
            ChallengeType.COLLECT_SHIELDS -> "Collect $target shields"
        }
}

object DailyChallengeManager {
    private var cachedDay: Int = -1
    private var challenges: List<DailyChallenge> = emptyList()

    private fun todayKey(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
    }

    fun getTodayChallenges(): List<DailyChallenge> {
        val today = todayKey()
        if (cachedDay == today && challenges.isNotEmpty()) {
            return challenges
        }
        cachedDay = today
        if (GameData.dailyChallengeDay == today) {
            challenges = GameData.loadChallenges()
            if (challenges.isNotEmpty()) return challenges
        }
        challenges = generateChallenges(today)
        GameData.dailyChallengeDay = today
        GameData.saveChallenges(challenges)
        return challenges
    }

    fun submitGameResult(
        starsCollected: Int,
        survivalSeconds: Int,
        missilesDestroyed: Int,
        shieldsCollected: Int
    ) {
        val today = todayKey()
        if (cachedDay != today) {
            getTodayChallenges()
        }
        for (ch in challenges) {
            if (ch.isCompleted) continue
            val delta = when (ch.type) {
                ChallengeType.COLLECT_STARS -> starsCollected
                ChallengeType.SURVIVE_SECONDS -> survivalSeconds
                ChallengeType.DESTROY_MISSILES -> missilesDestroyed
                ChallengeType.COLLECT_SHIELDS -> shieldsCollected
            }
            ch.progress = (ch.progress + delta).coerceAtMost(ch.target)
        }
        GameData.saveChallenges(challenges)
        for (ch in challenges) {
            if (ch.isCompleted && !GameData.isChallengeRewarded(ch)) {
                GameData.totalStars += ch.rewardStars
                GameData.addCoins(ch.rewardStars)
                GameData.markChallengeRewarded(ch)
            }
        }
    }

    private fun generateChallenges(seed: Int): List<DailyChallenge> {
        val types = ChallengeType.entries
        val result = mutableListOf<DailyChallenge>()
        val chosen = mutableSetOf<ChallengeType>()
        for (i in 0 until 3) {
            val hash = (seed.toLong() * 31L + i.toLong() * 73856093L and 0x7FFFFFFF).toInt()
            var t = types[hash % types.size]
            while (chosen.contains(t)) {
                t = types[(types.indexOf(t) + 1) % types.size]
            }
            chosen.add(t)
            val target = when (t) {
                ChallengeType.COLLECT_STARS -> 20 + (hash % 4) * 10
                ChallengeType.SURVIVE_SECONDS -> 30 + (hash % 5) * 15
                ChallengeType.DESTROY_MISSILES -> 10 + (hash % 4) * 5
                ChallengeType.COLLECT_SHIELDS -> 3 + (hash % 3)
            }
            result.add(DailyChallenge(t, target, 0))
        }
        return result
    }
}
