package com.autorizz.dating.tools

import com.autorizz.dating.prefs.PreferencesRepository
import com.cellclaw.tools.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject

class DatingPrefsSetTool @Inject constructor(
    private val prefsRepo: PreferencesRepository
) : Tool {
    override val name = "dating.prefs.set"
    override val description = "Set or update a swipe preference. Keys: age_min, age_max, gender, interests, deal_breakers, photo_preferences, bio_keywords, vibe, conversation_style, date_preferences, schedule_availability, distance"
    override val parameters = ToolParameters(
        properties = mapOf(
            "key" to ParameterProperty("string", "Preference key (e.g. age_min, interests, deal_breakers)"),
            "value" to ParameterProperty("string", "Preference value")
        ),
        required = listOf("key", "value")
    )
    override val requiresApproval = false

    override suspend fun execute(params: JsonObject): ToolResult {
        val key = params["key"]?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.error("Missing 'key' parameter")
        val value = params["value"]?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.error("Missing 'value' parameter")

        return try {
            prefsRepo.set(key, value)
            ToolResult.success(buildJsonObject {
                put("set", true)
                put("key", key)
                put("value", value)
            })
        } catch (e: Exception) {
            ToolResult.error("Failed to set preference: ${e.message}")
        }
    }
}

class DatingPrefsGetTool @Inject constructor(
    private val prefsRepo: PreferencesRepository
) : Tool {
    override val name = "dating.prefs.get"
    override val description = "Retrieve swipe preferences. If key is provided, returns that specific preference. Otherwise returns all preferences."
    override val parameters = ToolParameters(
        properties = mapOf(
            "key" to ParameterProperty("string", "Optional: specific preference key to retrieve")
        )
    )
    override val requiresApproval = false

    override suspend fun execute(params: JsonObject): ToolResult {
        val key = params["key"]?.jsonPrimitive?.contentOrNull

        return try {
            if (key != null) {
                val value = prefsRepo.get(key)
                ToolResult.success(buildJsonObject {
                    put("key", key)
                    put("value", value ?: "not set")
                })
            } else {
                val all = prefsRepo.getAll()
                ToolResult.success(buildJsonObject {
                    all.forEach { (k, v) -> put(k, v) }
                })
            }
        } catch (e: Exception) {
            ToolResult.error("Failed to get preferences: ${e.message}")
        }
    }
}
