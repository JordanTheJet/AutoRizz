package com.autorizz.dating.db

import androidx.room.*

@Database(
    entities = [
        SwipePreferenceEntity::class,
        MatchEntity::class,
        ConversationMessageEntity::class,
        ScheduledDateEntity::class,
        SwipeSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DatingDb : RoomDatabase() {
    abstract fun swipePreferenceDao(): SwipePreferenceDao
    abstract fun matchDao(): MatchDao
    abstract fun conversationMessageDao(): ConversationMessageDao
    abstract fun scheduledDateDao(): ScheduledDateDao
    abstract fun swipeSessionDao(): SwipeSessionDao
}

// ── Swipe Preferences ──

@Entity(tableName = "swipe_preferences")
data class SwipePreferenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "value") val value: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

// ── Matches ──

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "app") val app: String,
    @ColumnInfo(name = "age") val age: Int? = null,
    @ColumnInfo(name = "bio_summary") val bioSummary: String? = null,
    @ColumnInfo(name = "profile_screenshot") val profileScreenshot: String? = null,
    @ColumnInfo(name = "status") val status: String = "new",
    @ColumnInfo(name = "matched_at") val matchedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_message_at") val lastMessageAt: Long? = null,
    @ColumnInfo(name = "synced_at") val syncedAt: Long? = null
)

// ── Conversation Messages ──

@Entity(
    tableName = "conversation_messages",
    foreignKeys = [ForeignKey(
        entity = MatchEntity::class,
        parentColumns = ["id"],
        childColumns = ["match_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("match_id")]
)
data class ConversationMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    @ColumnInfo(name = "match_id") val matchId: Long,
    @ColumnInfo(name = "direction") val direction: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

// ── Scheduled Dates ──

@Entity(
    tableName = "scheduled_dates",
    foreignKeys = [ForeignKey(
        entity = MatchEntity::class,
        parentColumns = ["id"],
        childColumns = ["match_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("match_id")]
)
data class ScheduledDateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    @ColumnInfo(name = "match_id") val matchId: Long,
    @ColumnInfo(name = "date_time") val dateTime: Long,
    @ColumnInfo(name = "location") val location: String? = null,
    @ColumnInfo(name = "calendar_event_id") val calendarEventId: String? = null,
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "status") val status: String = "scheduled"
)

// ── Swipe Sessions ──

@Entity(tableName = "swipe_sessions")
data class SwipeSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "app") val app: String,
    @ColumnInfo(name = "started_at") val startedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "ended_at") val endedAt: Long? = null,
    @ColumnInfo(name = "profiles_seen") val profilesSeen: Int = 0,
    @ColumnInfo(name = "likes") val likes: Int = 0,
    @ColumnInfo(name = "passes") val passes: Int = 0,
    @ColumnInfo(name = "hit_limit") val hitLimit: Boolean = false
)
