package com.dpjha.slidingnumbersplus.services

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.dpjha.slidingnumbersplus.R
import com.dpjha.slidingnumbersplus.game.GameProgress
import com.google.android.gms.games.PlayGames

class PlayGamesClient(private val activity: Activity) {
    private val appId get() = activity.getString(R.string.games_app_id).trim()
    private val leaderboardId get() = activity.getString(R.string.gpgs_leaderboard_best_solve_score).trim()

    fun isConfigured(): Boolean = appId.toLongOrNull()?.let { it > 0 } == true

    fun signIn() {
        if (!requireConfiguration()) return
        PlayGames.getGamesSignInClient(activity).signIn()
            .addOnSuccessListener { result ->
                val message = if (result.isAuthenticated) {
                    "Connected to Google Play Games"
                } else {
                    "Play Games sign-in was not completed"
                }
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { showFailure("Play Games sign-in failed", it) }
    }

    fun showLeaderboards() {
        if (!requireConfiguration()) return
        if (leaderboardId.isEmpty()) {
            showNotConfigured()
            return
        }
        PlayGames.getLeaderboardsClient(activity).getLeaderboardIntent(leaderboardId)
            .addOnSuccessListener(activity::startActivity)
            .addOnFailureListener { showFailure("Could not open the leaderboard", it) }
    }

    fun showAchievements() {
        if (!requireConfiguration()) return
        PlayGames.getAchievementsClient(activity).getAchievementsIntent()
            .addOnSuccessListener(activity::startActivity)
            .addOnFailureListener { showFailure("Could not open achievements", it) }
    }

    fun recordWin(progress: GameProgress) {
        if (!isConfigured()) return
        if (leaderboardId.isNotEmpty()) {
            PlayGames.getLeaderboardsClient(activity)
                .submitScoreImmediate(leaderboardId, progress.totalScore)
                .addOnFailureListener { showFailure("Could not submit leaderboard score", it) }
        }

        val achievementIds = buildList {
            if (progress.levelsCompleted >= 1) add(R.string.gpgs_achievement_first_win)
            if (progress.bestStars >= 3) add(R.string.gpgs_achievement_three_stars)
            if (progress.maxLevel >= 6) add(R.string.gpgs_achievement_london_master)
            if (progress.maxLevel >= 11) add(R.string.gpgs_achievement_paris_master)
            if (progress.maxLevel >= 16) add(R.string.gpgs_achievement_new_york_master)
            if (progress.levelsCompleted >= 25) add(R.string.gpgs_achievement_25_levels)
        }
        val client = PlayGames.getAchievementsClient(activity)
        achievementIds.forEach { resourceId ->
            val id = activity.getString(resourceId).trim()
            if (id.isNotEmpty()) {
                client.unlockImmediate(id).addOnFailureListener {
                    Log.w(TAG, "Could not unlock configured Play Games achievement", it)
                }
            }
        }
    }

    private fun requireConfiguration(): Boolean {
        if (isConfigured()) return true
        showNotConfigured()
        return false
    }

    private fun showNotConfigured() {
        Toast.makeText(
            activity,
            "Link Google Play Games in the Play Console and add its IDs to the app resources.",
            Toast.LENGTH_LONG,
        ).show()
    }

    private fun showFailure(message: String, error: Exception) {
        Log.w(TAG, message, error)
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val TAG = "PlayGamesClient"
    }
}
