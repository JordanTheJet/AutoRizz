package com.autorizz.dating.convo

import android.util.Log
import com.autorizz.dating.DatingConfig
import com.autorizz.dating.db.MatchEntity
import com.autorizz.dating.match.MatchRepository
import com.autorizz.dating.prefs.PreferencesEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConvoManager @Inject constructor(
    private val convoRepo: ConversationRepository,
    private val matchRepo: MatchRepository,
    private val prefsEngine: PreferencesEngine,
    private val timingEngine: TimingEngine,
    private val datingConfig: DatingConfig
) {
    /**
     * Log a sent or received message for a match.
     */
    suspend fun logMessage(matchId: Long, direction: String, content: String): Long {
        val msgId = convoRepo.log(matchId, direction, content)
        matchRepo.updateLastMessage(matchId)

        // Update match status to conversing if it was new
        val match = matchRepo.getById(matchId)
        if (match?.status == "new") {
            matchRepo.updateStatus(matchId, "conversing")
        }

        Log.d(TAG, "Logged $direction message for match $matchId")
        return msgId
    }

    /**
     * Get the full conversation history for a match, formatted for LLM context.
     */
    suspend fun getConversationContext(matchId: Long): String {
        val messages = convoRepo.getHistory(matchId)
        if (messages.isEmpty()) return "No messages exchanged yet."

        return buildString {
            appendLine("Conversation history:")
            messages.forEach { msg ->
                val sender = if (msg.direction == "sent") "You" else "Them"
                appendLine("$sender: ${msg.content}")
            }
        }
    }

    /**
     * Check if it's appropriate to send a reply right now.
     */
    fun shouldReplyNow(): Boolean = timingEngine.isWithinActiveHours()

    /**
     * Get the delay before sending a reply (in minutes).
     */
    fun getReplyDelay(): Int = timingEngine.getRandomDelay()

    /**
     * Check if a conversation has gone stale (no messages for 48 hours).
     */
    suspend fun isConversationStale(matchId: Long): Boolean {
        val match = matchRepo.getById(matchId) ?: return true
        val lastMsg = match.lastMessageAt ?: match.matchedAt
        val hoursSinceLastMessage = (System.currentTimeMillis() - lastMsg) / (1000 * 60 * 60)
        return hoursSinceLastMessage >= 48
    }

    /**
     * Get the number of messages exchanged with a match.
     */
    suspend fun messageCount(matchId: Long): Int = convoRepo.messageCount(matchId)

    companion object {
        private const val TAG = "ConvoManager"
    }
}
