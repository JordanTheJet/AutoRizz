package com.autorizz.dating.date

import android.util.Log
import com.autorizz.dating.convo.ConversationRepository
import com.autorizz.dating.db.MatchEntity
import com.autorizz.dating.match.MatchRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DateScheduler @Inject constructor(
    private val dateRepo: DateRepository,
    private val matchRepo: MatchRepository,
    private val convoRepo: ConversationRepository
) {
    /**
     * Schedule a date with a match. Creates a record in the dates table.
     * Calendar event creation is handled by the dating.date.schedule tool
     * which delegates to calendar.create.
     */
    suspend fun schedule(
        matchId: Long,
        dateTime: Long,
        location: String?,
        notes: String? = null
    ): Long {
        val match = matchRepo.getById(matchId)
            ?: throw IllegalArgumentException("Match $matchId not found")

        // Build notes from conversation context if not provided
        val dateNotes = notes ?: buildDateNotes(match)

        val dateId = dateRepo.schedule(
            matchId = matchId,
            dateTime = dateTime,
            location = location,
            notes = dateNotes
        )

        // Update match status
        matchRepo.updateStatus(matchId, "date_scheduled")

        Log.d(TAG, "Date scheduled with ${match.name}: id=$dateId")
        return dateId
    }

    suspend fun setCalendarEventId(dateId: Long, eventId: String) {
        dateRepo.setCalendarEventId(dateId, eventId)
    }

    suspend fun markCompleted(dateId: Long) = dateRepo.updateStatus(dateId, "completed")

    suspend fun markCancelled(dateId: Long) = dateRepo.updateStatus(dateId, "cancelled")

    suspend fun markNoShow(dateId: Long) = dateRepo.updateStatus(dateId, "no_show")

    suspend fun getUpcoming() = dateRepo.getUpcoming()

    suspend fun getPast() = dateRepo.getPast()

    private suspend fun buildDateNotes(match: MatchEntity): String {
        val messages = convoRepo.getHistory(match.id)
        return buildString {
            appendLine("Match: ${match.name}")
            appendLine("App: ${match.app}")
            match.age?.let { appendLine("Age: $it") }
            match.bioSummary?.let { appendLine("Profile: $it") }
            if (messages.isNotEmpty()) {
                appendLine()
                appendLine("Conversation highlights:")
                // Include last few messages for context
                messages.takeLast(6).forEach { msg ->
                    val sender = if (msg.direction == "sent") "You" else match.name
                    appendLine("$sender: ${msg.content}")
                }
            }
        }
    }

    companion object {
        private const val TAG = "DateScheduler"
    }
}
