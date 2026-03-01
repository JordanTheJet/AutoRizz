package com.autorizz.dating.swipe

import android.util.Log
import com.autorizz.dating.DatingConfig
import com.autorizz.dating.match.MatchTracker
import com.autorizz.dating.prefs.PreferencesEngine
import javax.inject.Inject
import javax.inject.Singleton

data class SwipeSessionResult(
    val app: String,
    val profilesSeen: Int,
    val likes: Int,
    val passes: Int,
    val hitLimit: Boolean
)

@Singleton
class SwipeEngine @Inject constructor(
    private val datingConfig: DatingConfig,
    private val sessionRepo: SwipeSessionRepository,
    private val prefsEngine: PreferencesEngine,
    private val matchTracker: MatchTracker,
    private val hingeStrategy: HingeStrategy,
    private val tinderStrategy: TinderStrategy,
    private val bumbleStrategy: BumbleStrategy
) {
    private val strategies: Map<String, SwipeStrategy> by lazy {
        mapOf(
            DatingConfig.APP_HINGE to hingeStrategy,
            DatingConfig.APP_TINDER to tinderStrategy,
            DatingConfig.APP_BUMBLE to bumbleStrategy
        )
    }

    fun getStrategy(app: String): SwipeStrategy? = strategies[app]

    suspend fun getEnabledAppsWithRemainingLikes(): List<Pair<String, Int>> {
        return datingConfig.enabledApps.mapNotNull { app ->
            val strategy = strategies[app] ?: return@mapNotNull null
            val todayLikes = sessionRepo.getTodayLikesForApp(app)
            val remaining = (strategy.dailyLikeLimit - todayLikes).coerceAtLeast(0)
            if (remaining > 0) app to remaining else null
        }
    }

    suspend fun getSwipeStats(): Map<String, SwipeSessionResult> {
        val results = mutableMapOf<String, SwipeSessionResult>()
        for (app in DatingConfig.ALL_APPS) {
            val sessions = sessionRepo.getTodayByApp(app)
            results[app] = SwipeSessionResult(
                app = app,
                profilesSeen = sessions.sumOf { it.profilesSeen },
                likes = sessions.sumOf { it.likes },
                passes = sessions.sumOf { it.passes },
                hitLimit = sessions.any { it.hitLimit }
            )
        }
        return results
    }

    companion object {
        private const val TAG = "SwipeEngine"
    }
}
