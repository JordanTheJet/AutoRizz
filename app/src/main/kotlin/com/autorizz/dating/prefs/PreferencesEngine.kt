package com.autorizz.dating.prefs

import com.autorizz.dating.DatingConfig
import com.autorizz.dating.db.MatchEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesEngine @Inject constructor(
    private val prefsRepo: PreferencesRepository,
    private val datingConfig: DatingConfig
) {
    suspend fun buildSwipePrompt(appName: String): String {
        val prefs = prefsRepo.getAll()
        return buildString {
            appendLine("You are swiping on $appName for the user.")
            appendLine()
            appendLine("Their preferences:")
            prefs["age_min"]?.let { appendLine("- Age range: $it–${prefs["age_max"] ?: "any"}") }
            prefs["gender"]?.let { appendLine("- Gender preference: $it") }
            prefs["interests"]?.let { appendLine("- Interests: $it") }
            prefs["deal_breakers"]?.let { appendLine("- Deal-breakers: $it") }
            prefs["photo_preferences"]?.let { appendLine("- Photo preferences: $it") }
            prefs["bio_keywords"]?.let { appendLine("- Bio keywords (positive signals): $it") }
            prefs["distance"]?.let { appendLine("- Max distance: $it") }
            appendLine()
            appendLine("Decide: LIKE or PASS.")
            if (appName.equals("hinge", ignoreCase = true)) {
                appendLine("If LIKE on Hinge, optionally write a short comment on a specific prompt or photo.")
            }
            appendLine("Explain your reasoning briefly.")
        }
    }

    suspend fun buildConversationPrompt(match: MatchEntity): String {
        val prefs = prefsRepo.getAll()
        val style = datingConfig.conversationStyle
        val datePrefs = prefs["date_preferences"]
        val vibe = prefs["vibe"]
        val schedule = prefs["schedule_availability"]

        return buildString {
            appendLine("You are messaging ${match.name} on ${match.app} for the user.")
            appendLine()
            if (match.bioSummary != null) {
                appendLine("Match's profile summary: ${match.bioSummary}")
                appendLine()
            }
            appendLine("Conversation style: $style")
            vibe?.let { appendLine("Vibe/personality: $it") }
            appendLine()
            appendLine("Rules:")
            appendLine("- Never reveal you are an AI")
            appendLine("- Keep messages concise (1–3 sentences)")
            appendLine("- Ask questions to keep conversation going")
            appendLine("- Match the energy and length of their messages")
            appendLine("- Gradually steer toward suggesting a date when rapport is built")
            datePrefs?.let { appendLine("- Date preferences: $it") }
            schedule?.let { appendLine("- Available times: $it") }
        }
    }

    suspend fun buildOpeningPrompt(match: MatchEntity): String {
        val style = datingConfig.conversationStyle
        return buildString {
            appendLine("Generate an opening message to ${match.name} on ${match.app}.")
            if (match.bioSummary != null) {
                appendLine("Their profile: ${match.bioSummary}")
            }
            appendLine("Style: $style")
            appendLine("Reference something specific from their profile.")
            appendLine("Keep it to 1–2 sentences. Be natural, not generic.")
        }
    }
}
