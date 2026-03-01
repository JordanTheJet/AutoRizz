package com.autorizz.dating.tools

import com.autorizz.dating.date.DateScheduler
import com.autorizz.dating.match.MatchRepository
import com.cellclaw.tools.*
import kotlinx.serialization.json.*
import javax.inject.Inject

class DatingDateScheduleTool @Inject constructor(
    private val dateScheduler: DateScheduler,
    private val matchRepo: MatchRepository
) : Tool {
    override val name = "dating.date.schedule"
    override val description = "Schedule a date with a match. Creates a record and can trigger calendar event creation. Requires user approval so they can confirm date details."
    override val parameters = ToolParameters(
        properties = mapOf(
            "match_id" to ParameterProperty("integer", "Match ID"),
            "date_time" to ParameterProperty("integer", "Date/time as Unix timestamp in milliseconds"),
            "location" to ParameterProperty("string", "Venue or area for the date"),
            "notes" to ParameterProperty("string", "Optional notes (profile summary, conversation highlights, topics to discuss)")
        ),
        required = listOf("match_id", "date_time", "location")
    )
    override val requiresApproval = true

    override suspend fun execute(params: JsonObject): ToolResult {
        val matchId = params["match_id"]?.jsonPrimitive?.longOrNull
            ?: return ToolResult.error("Missing 'match_id' parameter")
        val dateTime = params["date_time"]?.jsonPrimitive?.longOrNull
            ?: return ToolResult.error("Missing 'date_time' parameter")
        val location = params["location"]?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.error("Missing 'location' parameter")
        val notes = params["notes"]?.jsonPrimitive?.contentOrNull

        return try {
            val match = matchRepo.getById(matchId)
                ?: return ToolResult.error("Match $matchId not found")

            val dateId = dateScheduler.schedule(
                matchId = matchId,
                dateTime = dateTime,
                location = location,
                notes = notes
            )

            ToolResult.success(buildJsonObject {
                put("scheduled", true)
                put("date_id", dateId)
                put("match_name", match.name)
                put("date_time", dateTime)
                put("location", location)
            })
        } catch (e: Exception) {
            ToolResult.error("Failed to schedule date: ${e.message}")
        }
    }
}
