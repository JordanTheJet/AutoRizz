package com.autorizz.dating.db

import androidx.room.*

@Dao
interface SwipePreferenceDao {
    @Query("SELECT * FROM swipe_preferences ORDER BY updated_at DESC")
    suspend fun getAll(): List<SwipePreferenceEntity>

    @Query("SELECT * FROM swipe_preferences WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): SwipePreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pref: SwipePreferenceEntity): Long

    @Query("DELETE FROM swipe_preferences WHERE `key` = :key")
    suspend fun delete(key: String)
}

@Dao
interface MatchDao {
    @Insert
    suspend fun insert(match: MatchEntity): Long

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getById(id: Long): MatchEntity?

    @Query("SELECT * FROM matches WHERE status = :status ORDER BY matched_at DESC")
    suspend fun getByStatus(status: String): List<MatchEntity>

    @Query("SELECT * FROM matches WHERE app = :app ORDER BY matched_at DESC")
    suspend fun getByApp(app: String): List<MatchEntity>

    @Query("SELECT * FROM matches ORDER BY matched_at DESC")
    suspend fun getAll(): List<MatchEntity>

    @Query("SELECT * FROM matches ORDER BY matched_at DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<MatchEntity>

    @Query("UPDATE matches SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE matches SET last_message_at = :timestamp WHERE id = :id")
    suspend fun updateLastMessage(id: Long, timestamp: Long)

    @Query("DELETE FROM matches WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM matches WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    @Query("SELECT COUNT(*) FROM matches WHERE app = :app")
    suspend fun countByApp(app: String): Int
}

@Dao
interface ConversationMessageDao {
    @Insert
    suspend fun insert(message: ConversationMessageEntity): Long

    @Query("SELECT * FROM conversation_messages WHERE match_id = :matchId ORDER BY timestamp ASC")
    suspend fun getByMatch(matchId: Long): List<ConversationMessageEntity>

    @Query("SELECT * FROM conversation_messages WHERE match_id = :matchId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentByMatch(matchId: Long, limit: Int = 20): List<ConversationMessageEntity>

    @Query("SELECT COUNT(*) FROM conversation_messages WHERE match_id = :matchId")
    suspend fun countByMatch(matchId: Long): Int
}

@Dao
interface ScheduledDateDao {
    @Insert
    suspend fun insert(date: ScheduledDateEntity): Long

    @Query("SELECT * FROM scheduled_dates WHERE match_id = :matchId")
    suspend fun getByMatch(matchId: Long): List<ScheduledDateEntity>

    @Query("SELECT * FROM scheduled_dates WHERE date_time > :now AND status = 'scheduled' ORDER BY date_time ASC")
    suspend fun getUpcoming(now: Long = System.currentTimeMillis()): List<ScheduledDateEntity>

    @Query("SELECT * FROM scheduled_dates WHERE date_time <= :now OR status != 'scheduled' ORDER BY date_time DESC")
    suspend fun getPast(now: Long = System.currentTimeMillis()): List<ScheduledDateEntity>

    @Query("UPDATE scheduled_dates SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE scheduled_dates SET calendar_event_id = :eventId WHERE id = :id")
    suspend fun updateCalendarEventId(id: Long, eventId: String)
}

@Dao
interface SwipeSessionDao {
    @Insert
    suspend fun insert(session: SwipeSessionEntity): Long

    @Query("SELECT * FROM swipe_sessions WHERE started_at > :dayStart ORDER BY started_at DESC")
    suspend fun getToday(dayStart: Long): List<SwipeSessionEntity>

    @Query("SELECT * FROM swipe_sessions WHERE app = :app AND started_at > :dayStart ORDER BY started_at DESC")
    suspend fun getTodayByApp(app: String, dayStart: Long): List<SwipeSessionEntity>

    @Query("SELECT * FROM swipe_sessions WHERE app = :app ORDER BY started_at DESC LIMIT :limit")
    suspend fun getByApp(app: String, limit: Int = 20): List<SwipeSessionEntity>

    @Update
    suspend fun update(session: SwipeSessionEntity)
}
