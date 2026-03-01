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
 * Tinder-specific swipe strategy.
 *
 * Key differences:
 * - ~100 right swipes per 12-hour period
 * - Gesture-based swiping (right = like, left = pass) for more natural behavior
 * - Aggressive premium upsell popups (Gold, Platinum) — must dismiss frequently
 * - Many profiles have minimal bios — may need vision.analyze for photo-based decisions
 * - Skip Top Picks (paid), never use Super Like (paid)
 */
class TinderStrategy @Inject constructor(
    private val toolRegistry: ToolRegistry,
    private val paywallDetector: PaywallDetector
) : SwipeStrategy {

    override val appName = DatingConfig.APP_TINDER
    override val appPackage = DatingConfig.APP_PACKAGES[DatingConfig.APP_TINDER]!!
    override val dailyLikeLimit = 100

    override suspend fun navigateToSwipeDeck() {
        toolRegistry.execute("app.launch", buildJsonObject {
            put("package_name", appPackage)
        })
        // Tinder opens to swipe deck by default, but dismiss any initial popups
        handlePopups()
    }

    override suspend fun readCurrentProfile(): ProfileData? {
        val screenResult = toolRegistry.execute("screen.read", buildJsonObject {})
        if (!screenResult.success) return null

        val screenText = screenResult.data.toString()

        return ProfileData(
            name = extractField(screenText, "name") ?: return null,
            age = extractField(screenText, "age")?.toIntOrNull(),
            bio = extractField(screenText, "bio"),
            distance = extractField(screenText, "distance"),
            interests = extractInterests(screenText)
        )
    }

    override suspend fun executeLike(comment: String?) {
        // Use swipe gesture for more natural behavior
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

        // Tinder-specific popups: "Get Tinder Gold", "Boost your profile"
        val tinderPopups = listOf(
            "get tinder gold", "get tinder platinum", "get tinder plus",
            "boost your profile", "see who likes you", "top picks"
        )
        if (tinderPopups.any { screenText.contains(it, ignoreCase = true) }) {
            paywallDetector.dismiss(toolRegistry)
            return true
        }

        return false
    }

    override suspend fun isAtLimit(): Boolean {
        val screenResult = toolRegistry.execute("screen.read", buildJsonObject {})
        if (!screenResult.success) return true

        val screenText = screenResult.data.toString()
        return screenText.contains("out of likes", ignoreCase = true) ||
                screenText.contains("no more likes", ignoreCase = true) ||
                screenText.contains("come back later", ignoreCase = true)
    }

    override suspend fun navigateToMatches() {
        toolRegistry.execute("app.automate", buildJsonObject {
            put("action", "tap")
            put("description", "Tap the Messages/chat icon to view matches and conversations")
        })
    }

    override suspend fun isMatchPopupShowing(): Boolean {
        val screenResult = toolRegistry.execute("screen.read", buildJsonObject {})
        if (!screenResult.success) return false
        val text = screenResult.data.toString()
        return text.contains("It's a Match", ignoreCase = true)
    }

    override suspend fun dismissMatchPopup() {
        toolRegistry.execute("app.automate", buildJsonObject {
            put("action", "tap")
            put("description", "Tap 'Keep Swiping' to dismiss the match popup")
        })
    }

    private fun extractField(screenText: String, field: String): String? = null
    private fun extractInterests(screenText: String): List<String> = emptyList()

    private suspend fun ToolRegistry.execute(toolName: String, params: JsonObject): ToolResult {
        val tool = get(toolName) ?: return ToolResult.error("Tool $toolName not found")
        return tool.execute(params)
    }
}
