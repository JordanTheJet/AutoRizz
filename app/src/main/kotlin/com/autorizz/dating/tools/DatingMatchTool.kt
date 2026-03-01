package com.autorizz.dating.tools

import com.autorizz.dating.match.MatchRepository
import com.cellclaw.tools.*
import kotlinx.serialization.json.*
import javax.inject.Inject

class DatingMatchRecordTool @Inject constructor(
    private val matchRepo: MatchRepository
) : Tool {
    override val name = "dating.match.record"
    override val description = "Record a new match from a dating app."
    override val parameters = ToolParameters(
        properties = mapOf(
            "name" to ParameterProperty("string", "Match's name"),
            "app" to ParameterProperty("string", "Dating app (hinge, tinder, bumble)"),
            "age" to ParameterProperty("integer", "Match's age (if visible)"),
            "bio_summary" to ParameterProperty("string", "Summary of their profile/bio")
        ),
        required = listOf("name", "app")
    )
    override val requiresApproval = false

    override suspend fun execute(params: JsonObject): ToolResult {
        val name = params["name"]?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.error("Missing 'name' parameter")
        val app = params["app"]?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.error("Missing 'app' parameter")
        val age = params["age"]?.jsonPrimitive?.intOrNull
        val bioSummary = params["bio_summary"]?.jsonPrimitive?.contentOrNull

        return try {
            val id = matchRepo.record(name = name, app = app, age = age, bioSummary = bioSummary)
            ToolResult.success(buildJsonObject {
                put("recorded", true)
                put("match_id", id)
                put("name", name)
                put("app", app)
            })
        } catch (e: Exception) {
            ToolResult.error("Failed to record match: ${e.message}")
        }
    }
}

class DatingMatchListTool @Inject constructor(
    private val matchRepo: MatchRepository
) : Tool {
    override val name = "dating.match.list"
    override val description = "List matches with optional filters by status or app."
    override val parameters = ToolParameters(
        properties = mapOf(
            "status" to ParameterProperty("string", "Filter by status: new, conversing, date_scheduled, date_completed, stale, unmatched"),
            "app" to ParameterProperty("string", "Filter by app: hinge, tinder, bumble"),
            "limit" to ParameterProperty("integer", "Max matches to return (default 20)")
        )
    )
    override val requiresApproval = false

    override suspend fun execute(params: JsonObject): ToolResult {
        val status = params["status"]?.jsonPrimitive?.contentOrNull
        val app = params["app"]?.jsonPrimitive?.contentOrNull
        val limit = params["limit"]?.jsonPrimitive?.intOrNull ?: 20

        return try {
            val matches = when {
                status != null -> matchRepo.getByStatus(status)
                app != null -> matchRepo.getByApp(app)
                else -> matchRepo.getRecent(limit)
            }.take(limit)

            val result = buildJsonArray {
                matches.forEach { match ->
                    add(buildJsonObject {
                        put("id", match.id)
                        put("name", match.name)
                        put("app", match.app)
                        match.age?.let { put("age", it) }
                        put("status", match.status)
                        match.bioSummary?.let { put("bio_summary", it) }
                        put("matched_at", match.matchedAt)
                        match.lastMessageAt?.let { put("last_message_at", it) }
                    })
                }
            }

            ToolResult.success(buildJsonObject {
                put("count", matches.size)
                put("matches", result)
            })
        } catch (e: Exception) {
            ToolResult.error("Failed to list matches: ${e.message}")
        }
    }
}

class DatingMatchUpdateTool @Inject constructor(
    private val matchRepo: MatchRepository
) : Tool {
    override val name = "dating.match.update"
    override val description = "Update a match's status."
    override val parameters = ToolParameters(
        properties = mapOf(
            "match_id" to ParameterProperty("integer", "Match ID to update"),
            "status" to ParameterProperty("string", "New status: new, conversing, date_scheduled, date_completed, stale, unmatched")
        ),
        required = listOf("match_id", "status")
    )
    override val requiresApproval = false

    override suspend fun execute(params: JsonObject): ToolResult {
        val matchId = params["match_id"]?.jsonPrimitive?.longOrNull
            ?: return ToolResult.error("Missing 'match_id' parameter")
        val status = params["status"]?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.error("Missing 'status' parameter")

        val validStatuses = setOf("new", "conversing", "date_scheduled", "date_completed", "stale", "unmatched")
        if (status !in validStatuses) {
            return ToolResult.error("Invalid status '$status'. Must be one of: ${validStatuses.joinToString()}")
        }

        return try {
            matchRepo.updateStatus(matchId, status)
            ToolResult.success(buildJsonObject {
                put("updated", true)
                put("match_id", matchId)
                put("status", status)
            })
        } catch (e: Exception) {
            ToolResult.error("Failed to update match: ${e.message}")
        }
    }
}
