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
 * Hinge-specific swipe strategy.
 *
 * Key differences from other apps:
 * - 8 free likes per day (resets at midnight)
 * - Can comment on specific prompts/photos when liking (free, increases match rate)
 * - Profile cards show prompts (3 per profile) — great for personalized openers
 * - Skip Standouts section (roses cost money)
 * - Use "Most Compatible" daily suggestion (free)
 */
class HingeStrategy @Inject constructor(
    private val toolRegistry: ToolRegistry,
    private val paywallDetector: PaywallDetector
) : SwipeStrategy {

    override val appName = DatingConfig.APP_HINGE
    override val appPackage = DatingConfig.APP_PACKAGES[DatingConfig.APP_HINGE]!!
    override val dailyLikeLimit = 8

    override suspend fun navigateToSwipeDeck() {
        // Launch Hinge
        toolRegistry.execute("app.launch", buildJsonObject {
            put("package_name", appPackage)
        })
        // Wait for app to load, then navigate to Discover tab
        toolRegistry.execute("app.automate", buildJsonObject {
            put("action", "tap")
            put("description", "Tap the Discover tab (heart icon) to navigate to swipe deck")
        })
    }

    override suspend fun readCurrentProfile(): ProfileData? {
        val screenResult = toolRegistry.execute("screen.read", buildJsonObject {})
        if (!screenResult.success) return null

        val screenText = screenResult.data.toString()

        // Parse profile data from accessibility tree
        // Hinge profiles typically show: name, age, location, prompts, photos
        return ProfileData(
            name = extractField(screenText, "name") ?: return null,
            age = extractField(screenText, "age")?.toIntOrNull(),
            bio = null, // Hinge doesn't have a traditional bio
            prompts = extractPrompts(screenText),
            distance = extractField(screenText, "distance"),
            location = extractField(screenText, "location")
        )
    }

    override suspend fun executeLike(comment: String?) {
        if (comment != null) {
            // Tap on a prompt/photo first to like with a comment
            toolRegistry.execute("app.automate", buildJsonObject {
                put("action", "tap")
                put("description", "Tap the like button on a prompt to add a comment")
            })
            // Type the comment
            toolRegistry.execute("app.automate", buildJsonObject {
                put("action", "type")
                put("text", comment)
                put("description", "Type comment on the liked prompt")
            })
            // Send the comment
            toolRegistry.execute("app.automate", buildJsonObject {
                put("action", "tap")
                put("description", "Tap send button to submit the like with comment")
            })
        } else {
            toolRegistry.execute("app.automate", buildJsonObject {
                put("action", "tap")
                put("description", "Tap the heart/like button to like this profile")
            })
        }
    }

    override suspend fun executePass() {
        toolRegistry.execute("app.automate", buildJsonObject {
            put("action", "tap")
            put("description", "Tap the X/skip button to pass on this profile")
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

        // Check for Standouts section (skip it — roses cost money)
        if (screenText.contains("Standout", ignoreCase = true) &&
            screenText.contains("Rose", ignoreCase = true)
        ) {
            toolRegistry.execute("app.automate", buildJsonObject {
                put("action", "tap")
                put("description", "Navigate away from Standouts section back to Discover")
            })
            return true
        }

        return false
    }

    override suspend fun isAtLimit(): Boolean {
        val screenResult = toolRegistry.execute("screen.read", buildJsonObject {})
        if (!screenResult.success) return true

        val screenText = screenResult.data.toString()
        return screenText.contains("out of likes", ignoreCase = true) ||
                screenText.contains("come back", ignoreCase = true) ||
                screenText.contains("no more likes", ignoreCase = true)
    }

    override suspend fun navigateToMatches() {
        toolRegistry.execute("app.automate", buildJsonObject {
            put("action", "tap")
            put("description", "Tap the Matches tab (chat icon) to view matches and conversations")
        })
    }

    override suspend fun isMatchPopupShowing(): Boolean {
        val screenResult = toolRegistry.execute("screen.read", buildJsonObject {})
        if (!screenResult.success) return false
        val text = screenResult.data.toString()
        return text.contains("It's a match", ignoreCase = true) ||
                text.contains("You matched", ignoreCase = true)
    }

    override suspend fun dismissMatchPopup() {
        toolRegistry.execute("app.automate", buildJsonObject {
            put("action", "tap")
            put("description", "Tap 'Keep Playing' or dismiss the match popup")
        })
    }

    private fun extractField(screenText: String, field: String): String? {
        // The agent loop handles actual parsing via LLM — these are placeholder extractors.
        // In practice, screen.read returns accessibility tree text that the LLM interprets.
        return null
    }

    private fun extractPrompts(screenText: String): List<PromptAnswer> = emptyList()

    private suspend fun ToolRegistry.execute(toolName: String, params: JsonObject): ToolResult {
        val tool = get(toolName) ?: return ToolResult.error("Tool $toolName not found")
        return tool.execute(params)
    }
}
