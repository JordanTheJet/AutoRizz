package com.autorizz.dating.paywall

import com.cellclaw.tools.ToolRegistry
import com.cellclaw.tools.ToolResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects paywall/premium upsell screens across dating apps
 * and dismisses them automatically.
 */
@Singleton
class PaywallDetector @Inject constructor() {

    private val paywallPatterns = listOf(
        // Generic premium upsells
        "get premium", "get gold", "get platinum", "upgrade",
        "go premium", "try premium", "unlock premium",
        // Out of likes / swipes
        "out of likes", "out of swipes", "no more likes",
        "come back later", "no more swipes", "daily limit",
        // Paid features
        "super like", "boost", "spotlight", "rose",
        "see who liked you", "see who likes you",
        // Price indicators
        "per month", "per week", "free trial",
        "/month", "/week", "/year",
        // Specific premium names
        "tinder gold", "tinder platinum", "tinder plus",
        "bumble premium", "bumble boost",
        "hinge preferred", "hingeX",
        // Paid feature prompts
        "send a rose", "send rose", "backtrack",
        "rewind", "undo swipe"
    )

    /**
     * Check if the current screen text indicates a paywall or premium upsell.
     */
    fun isPaywallScreen(screenText: String): Boolean {
        val lower = screenText.lowercase()
        return paywallPatterns.any { lower.contains(it) }
    }

    /**
     * Check if the screen indicates the free swipe limit has been reached.
     */
    fun isOutOfLikes(screenText: String): Boolean {
        val lower = screenText.lowercase()
        val limitPatterns = listOf(
            "out of likes", "out of swipes", "no more likes",
            "no more swipes", "come back later", "daily limit"
        )
        return limitPatterns.any { lower.contains(it) }
    }

    /**
     * Attempt to dismiss a paywall popup via various methods.
     * Tries: tap X/close button, tap outside the modal, press back.
     */
    suspend fun dismiss(toolRegistry: ToolRegistry) {
        // Try tapping close/X button first
        val closeResult = toolRegistry.execute("app.automate", buildJsonObject {
            put("action", "tap")
            put("description", "Tap the X or close button to dismiss the premium popup")
        })

        // If that didn't work, try pressing back
        if (!closeResult.success) {
            toolRegistry.execute("app.automate", buildJsonObject {
                put("action", "press_back")
                put("description", "Press back to dismiss the premium popup")
            })
        }
    }

    private suspend fun ToolRegistry.execute(toolName: String, params: JsonObject): ToolResult {
        val tool = get(toolName) ?: return ToolResult.error("Tool $toolName not found")
        return tool.execute(params)
    }
}
