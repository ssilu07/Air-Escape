package com.royals.airescape.engine

import android.app.Activity
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory

object RatingManager {
    private var activity: Activity? = null
    private var reviewInfo: ReviewInfo? = null
    private var sessionCount = 0

    fun init(activity: Activity) {
        this.activity = activity
        prefetchReviewInfo()
    }

    private fun prefetchReviewInfo() {
        val act = activity ?: return
        val manager = ReviewManagerFactory.create(act)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                reviewInfo = task.result
            }
        }
    }

    fun onGameOver(score: Int) {
        sessionCount++
        if (sessionCount >= 5 && score >= 50) {
            launchReviewFlow()
            sessionCount = 0
        }
    }

    private fun launchReviewFlow() {
        val act = activity ?: return
        val info = reviewInfo ?: run {
            prefetchReviewInfo()
            return
        }
        val manager = ReviewManagerFactory.create(act)
        manager.launchReviewFlow(act, info).addOnCompleteListener {
            prefetchReviewInfo()
        }
    }

    fun showReview() {
        val act = activity ?: return
        val info = reviewInfo
        if (info != null) {
            val manager = ReviewManagerFactory.create(act)
            manager.launchReviewFlow(act, info).addOnCompleteListener {
                prefetchReviewInfo()
            }
        } else {
            prefetchReviewInfo()
        }
    }

    fun release() {
        activity = null
        reviewInfo = null
    }
}
