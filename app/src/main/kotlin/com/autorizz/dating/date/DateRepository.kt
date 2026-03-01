package com.autorizz.dating.date

import com.autorizz.dating.db.ScheduledDateDao
import com.autorizz.dating.db.ScheduledDateEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DateRepository @Inject constructor(
    private val dao: ScheduledDateDao
) {
    suspend fun schedule(
        matchId: Long,
        dateTime: Long,
        location: String? = null,
        notes: String? = null
    ): Long = dao.insert(
        ScheduledDateEntity(
            matchId = matchId,
            dateTime = dateTime,
            location = location,
            notes = notes
        )
    )

    suspend fun getByMatch(matchId: Long): List<ScheduledDateEntity> =
        dao.getByMatch(matchId)

    suspend fun getUpcoming(): List<ScheduledDateEntity> = dao.getUpcoming()

    suspend fun getPast(): List<ScheduledDateEntity> = dao.getPast()

    suspend fun updateStatus(id: Long, status: String) = dao.updateStatus(id, status)

    suspend fun setCalendarEventId(id: Long, eventId: String) =
        dao.updateCalendarEventId(id, eventId)
}
