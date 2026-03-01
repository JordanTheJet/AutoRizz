package com.autorizz.dating.match

import android.util.Log
import com.autorizz.dating.DatingConfig
import com.autorizz.dating.db.MatchEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatchTracker @Inject constructor(
    private val matchRepo: MatchRepository,
    private val datingConfig: DatingConfig
) {
    suspend fun recordMatch(
        name: String,
        app: String,
        age: Int? = null,
        bioSummary: String? = null,
        profileScreenshot: String? = null
    ): Long {
        Log.d(TAG, "New match recorded: $name on $app")
        return matchRepo.record(
            name = name,
            app = app,
            age = age,
            bioSummary = bioSummary,
            profileScreenshot = profileScreenshot
        )
    }

    suspend fun getNewMatches(): List<MatchEntity> =
        matchRepo.getByStatus("new")

    suspend fun getActiveConversations(): List<MatchEntity> =
        matchRepo.getByStatus("conversing")

    suspend fun getScheduledDates(): List<MatchEntity> =
        matchRepo.getByStatus("date_scheduled")

    suspend fun markConversing(matchId: Long) =
        matchRepo.updateStatus(matchId, "conversing")

    suspend fun markDateScheduled(matchId: Long) =
        matchRepo.updateStatus(matchId, "date_scheduled")

    suspend fun markDateCompleted(matchId: Long) =
        matchRepo.updateStatus(matchId, "date_completed")

    suspend fun markStale(matchId: Long) =
        matchRepo.updateStatus(matchId, "stale")

    suspend fun markUnmatched(matchId: Long) =
        matchRepo.updateStatus(matchId, "unmatched")

    suspend fun getMatchStats(): MatchStats {
        return MatchStats(
            total = matchRepo.getAll().size,
            newCount = matchRepo.countByStatus("new"),
            conversingCount = matchRepo.countByStatus("conversing"),
            dateScheduledCount = matchRepo.countByStatus("date_scheduled"),
            staleCount = matchRepo.countByStatus("stale"),
            hingeCount = matchRepo.countByApp(DatingConfig.APP_HINGE),
            tinderCount = matchRepo.countByApp(DatingConfig.APP_TINDER),
            bumbleCount = matchRepo.countByApp(DatingConfig.APP_BUMBLE)
        )
    }

    companion object {
        private const val TAG = "MatchTracker"
    }
}

data class MatchStats(
    val total: Int,
    val newCount: Int,
    val conversingCount: Int,
    val dateScheduledCount: Int,
    val staleCount: Int,
    val hingeCount: Int,
    val tinderCount: Int,
    val bumbleCount: Int
)
