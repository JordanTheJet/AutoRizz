package com.autorizz.dating.sync

import android.util.Log
import com.autorizz.dating.db.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles syncing dating-specific entities between local Room DB and Supabase.
 * Used by the SyncEngine when in Pro mode.
 *
 * Sync strategy per entity type (from PRD):
 * - swipe_preferences: Bidirectional, latest-write-wins
 * - matches: Bidirectional, latest-write-wins
 * - conversation_messages: Bidirectional, append-only (messages never edited)
 * - scheduled_dates: Bidirectional, latest-write-wins
 * - swipe_sessions: Push-only (device → cloud), no conflict
 */
@Singleton
class DatingSyncAdapter @Inject constructor(
    private val prefDao: SwipePreferenceDao,
    private val matchDao: MatchDao,
    private val messageDao: ConversationMessageDao,
    private val dateDao: ScheduledDateDao,
    private val sessionDao: SwipeSessionDao
) {
    /**
     * Get all dating entities that need to be pushed to the server.
     * Returns data formatted for the sync service.
     */
    suspend fun getLocalChanges(since: Long): DatingSyncPayload {
        return DatingSyncPayload(
            preferences = prefDao.getAll().filter { it.updatedAt > since },
            matches = matchDao.getAll().filter { (it.syncedAt ?: 0) < it.matchedAt },
            messages = emptyList(), // Messages are append-only, tracked separately
            dates = emptyList(),
            sessions = emptyList()
        )
    }

    /**
     * Apply changes received from the server to local database.
     */
    suspend fun applyRemoteChanges(payload: DatingSyncPayload) {
        // Preferences: latest-write-wins
        payload.preferences.forEach { pref ->
            val local = prefDao.getByKey(pref.key)
            if (local == null || pref.updatedAt > local.updatedAt) {
                prefDao.upsert(pref)
            }
        }

        // Matches: latest-write-wins
        payload.matches.forEach { match ->
            val local = if (match.remoteId != null) {
                matchDao.getAll().find { it.remoteId == match.remoteId }
            } else null

            if (local == null) {
                matchDao.insert(match)
            } else if (match.matchedAt > local.matchedAt) {
                matchDao.updateStatus(local.id, match.status)
            }
        }

        // Messages: append-only
        payload.messages.forEach { msg ->
            val existing = messageDao.getByMatch(msg.matchId)
                .find { it.remoteId == msg.remoteId }
            if (existing == null) {
                messageDao.insert(msg)
            }
        }

        Log.d(TAG, "Applied remote dating changes: " +
                "${payload.preferences.size} prefs, " +
                "${payload.matches.size} matches, " +
                "${payload.messages.size} messages")
    }

    companion object {
        private const val TAG = "DatingSyncAdapter"
    }
}

data class DatingSyncPayload(
    val preferences: List<SwipePreferenceEntity> = emptyList(),
    val matches: List<MatchEntity> = emptyList(),
    val messages: List<ConversationMessageEntity> = emptyList(),
    val dates: List<ScheduledDateEntity> = emptyList(),
    val sessions: List<SwipeSessionEntity> = emptyList()
)
