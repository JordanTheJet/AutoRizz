package com.autorizz.dating.tools

import com.autorizz.dating.convo.ConvoManager
import com.autorizz.dating.convo.ConversationRepository
import com.cellclaw.tools.*
import kotlinx.serialization.json.*
import javax.inject.Inject

class DatingConvoLogTool @Inject constructor(
    private val convoManager: ConvoManager
) : Tool {
    override val name = "dating.convo.log"
    override val description = "Log a message sent to or received from a match."
    override val parameters = ToolParameters(
        properties = mapOf(
            "match_id" to ParameterProperty("integer", "Match ID"),
            "direction" to ParameterProperty("string", "Message direction: sent or received"),
            "content" to ParameterProperty("string", "Message text content")
        ),
        required = listOf("match_id", "direction", "content")
    )
    override val requiresApproval = false

    override suspend fun execute(params: JsonObject): ToolResult {
        val matchId = params["match_id"]?.jsonPrimitive?.longOrNull
            ?: return ToolResult.error("Missing 'match_id' parameter")
        val direction = params["direction"]?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.error("Missing 'direction' parameter")
        val content = params["content"]?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.error("Missing 'content' parameter")

        if (direction !in setOf("sent", "received")) {
            return ToolResult.error("Invalid direction '$direction'. Must be 'sent' or 'received'.")
        }

        return try {
            val msgId = convoManager.logMessage(matchId, direction, content)
            ToolResult.success(buildJsonObject {
                put("logged", true)
                put("message_id", msgId)
                put("match_id", matchId)
                put("direction", direction)
            })
        } catch (e: Exception) {
            ToolResult.error("Failed to log message: ${e.message}")
        }
    }
}

class DatingConvoHistoryTool @Inject constructor(
    private val convoRepo: ConversationRepository
) : Tool {
    override val name = "dating.convo.history"
    override val description = "Retrieve conversation history with a match."
    override val parameters = ToolParameters(
        properties = mapOf(
            "match_id" to ParameterProperty("integer", "Match ID"),
            "limit" to ParameterProperty("integer", "Max messages to return (default 20)")
        ),
        required = listOf("match_id")
    )
    override val requiresApproval = false

    override suspend fun execute(params: JsonObject): ToolResult {
        val matchId = params["match_id"]?.jsonPrimitive?.longOrNull
            ?: return ToolResult.error("Missing 'match_id' parameter")
        val limit = params["limit"]?.jsonPrimitive?.intOrNull ?: 20

        return try {
            val messages = convoRepo.getRecent(matchId, limit)

            val result = buildJsonArray {
                messages.forEach { msg ->
                    add(buildJsonObject {
                        put("id", msg.id)
                        put("direction", msg.direction)
                        put("content", msg.content)
                        put("timestamp", msg.timestamp)
                    })
                }
            }

            ToolResult.success(buildJsonObject {
                put("match_id", matchId)
                put("count", messages.size)
                put("messages", result)
            })
        } catch (e: Exception) {
            ToolResult.error("Failed to get conversation history: ${e.message}")
        }
    }
}
