package com.autorizz.dating.swipe

import com.autorizz.dating.DatingConfig
import com.autorizz.dating.paywall.PaywallDetector
import com.cellclaw.tools.ToolRegistry
import com.cellclaw.tools.ToolResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

/**
 * Bumble-specific swipe strategy.
 *
 * Key differences:
 * - ~25 right swipes per day (resets at midnight)
 * - Women must message first within 24 hours, or match expires
 * - Profile prompts similar to Hinge — good for personalized openers
 * - Interest badges (Hiking, Foodie, etc.) factor into swipe decisions
 * - Never tap Backtrack (paid), skip Compliments (paid on free tier)
 */
class BumbleStrategy @Inject constructor(
    private val toolRegistry: ToolRegistry,
    private val paywallDetector: PaywallDetector
) : SwipeStrategy {

    override val appName = DatingConfig.APP_BUMBLE
    override val appPackage = DatingConfig.APP_PACKAGES[DatingConfig.APP_BUMBLE]!!
    override val dailyLikeLimit = 25

    override suspend fun navigateToSwipeDeck() {
        toolRegistry.execute("app.launch", buildJsonObject {
            put("package_name", appPackage)
        })
        // Navigate to the dating/swipe section (Bumble has Date, BFF, Bizz modes)
        toolRegistry.execute("app.automate", buildJsonObject {
            put("action", "tap")
            put("description", "Tap the swipe/discover area to start swiping on profiles")
        })
    }

    override suspend fun readCurrentProfile(): ProfileData? {
        val screenResult = toolRegistry.execute("screen.read", buildJsonObject {})
        if (!screenResult.success) return null

        val screenText = screenResult.data.toString()

        return ProfileData(
            name = extractField(screenText, "name") ?: return null,
            age = extractField(screenText, "age")?.toIntOrNull(),
            bio = extractField(screenText, "bio"),
            prompts = extractPrompts(screenText),
            interests = extractBadges(screenText),
            distance = extractField(screenText, "distance")
        )
    }

    override suspend fun executeLike(comment: String?) {
        toolRegistry.execute("app.automate", buildJsonObject {
            put("action", "swipe")
            put("direction", "right")
            put("description", "Swipe right to like this profile")
        })
    }

    override suspend fun executePass() {
        toolRegistry.execute("app.automate", buildJsonObject {
            put("action", "swipe")
            put("direction", "left")
            put("description", "Swipe left to pass on this profile")
        })
    }

    override suspend fun handlePopups(): Boolean {
        val screenResult = toolRegistry.execute("screen.read", buildJsonObject {})
        if (!screenResult.success) return false

        val screenText = screenResult.data.toString()

        if (paywallDetector.isPaywallScreen(screenText)) {
            paywallDetector.dismiss(toolRegistry)
            return true
        }

        // Bumble-specific: Boost, Spotlight, Compliments
        val bumblePopups = listOf(
            "boost your profile", "spotlight", "send a compliment",
            "get bumble premium", "bumble boost", "backtrack"
        )
        if (bumblePopups.any { screenText.contains(it, ignoreCase = true) }) {
            paywallDetector.dismiss(toolRegistry)
            return true
        }

        return false
    }

    override suspend fun isAtLimit(): Boolean {
        val screenResult = toolRegistry.execute("screen.read", buildJsonObject {})
        if (!screenResult.success) return true

        val screenText = screenResult.data.toString()
        return screenText.contains("out of", ignoreCase = true) ||
                screenText.contains("no more", ignoreCase = true) ||
                screenText.contains("daily limit", ignoreCase = true) ||
                screenText.contains("come back", ignoreCase = true)
    }

    override suspend fun navigateToMatches() {
        toolRegistry.execute("app.automate", buildJsonObject {
            put("action", "tap")
            put("description", "Tap the chat/conversations icon to view matches")
        })
    }

    override suspend fun isMatchPopupShowing(): Boolean {
        val screenResult = toolRegistry.execute("screen.read", buildJsonObject {})
        if (!screenResult.success) return false
        val text = screenResult.data.toString()
        return text.contains("You matched", ignoreCase = true) ||
                text.contains("It's a match", ignoreCase = true)
    }

    override suspend fun dismissMatchPopup() {
        toolRegistry.execute("app.automate", buildJsonObject {
            put("action", "tap")
            put("description", "Dismiss the match popup and continue swiping")
        })
    }

    private fun extractField(screenText: String, field: String): String? = null
    private fun extractPrompts(screenText: String): List<PromptAnswer> = emptyList()
    private fun extractBadges(screenText: String): List<String> = emptyList()

    private suspend fun ToolRegistry.execute(toolName: String, params: JsonObject): ToolResult {
        val tool = get(toolName) ?: return ToolResult.error("Tool $toolName not found")
        return tool.execute(params)
    }
}
