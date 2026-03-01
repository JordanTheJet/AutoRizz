package com.autorizz.dating.swipe

import com.autorizz.dating.db.SwipeSessionDao
import com.autorizz.dating.db.SwipeSessionEntity
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SwipeSessionRepository @Inject constructor(
    private val dao: SwipeSessionDao
) {
    suspend fun startSession(app: String): Long =
        dao.insert(SwipeSessionEntity(app = app))

    suspend fun endSession(session: SwipeSessionEntity) =
        dao.update(session.copy(endedAt = System.currentTimeMillis()))

    suspend fun updateSession(session: SwipeSessionEntity) = dao.update(session)

    suspend fun getTodaySessions(): List<SwipeSessionEntity> =
        dao.getToday(todayStart())

    suspend fun getTodayByApp(app: String): List<SwipeSessionEntity> =
        dao.getTodayByApp(app, todayStart())

    suspend fun getTodayLikesForApp(app: String): Int =
        getTodayByApp(app).sumOf { it.likes }

    suspend fun getByApp(app: String, limit: Int = 20): List<SwipeSessionEntity> =
        dao.getByApp(app, limit)

    private fun todayStart(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
