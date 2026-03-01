package com.autorizz.proxy

import android.util.Log
import com.autorizz.auth.AuthManager
import com.autorizz.backend.ProxyService
import com.autorizz.credits.AiMode
import com.autorizz.credits.CreditCalculator
import com.autorizz.credits.CreditManager
import com.autorizz.credits.InsufficientCreditsException
import com.autorizz.mode.AutoRizzConfig
import com.cellclaw.provider.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provider that routes LLM requests through the AutoRizz server proxy.
 * Used in Pro mode. The server holds the API keys and deducts credits atomically.
 */
@Singleton
class ProxyProvider @Inject constructor(
    private val authManager: AuthManager,
    private val creditManager: CreditManager,
    private val proxyService: ProxyService,
    private val appConfig: com.cellclaw.config.AppConfig,
    private val cellBreakConfig: AutoRizzConfig
) : Provider {
    override val name = "autorizz-proxy"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun complete(request: CompletionRequest): CompletionResponse {
        creditManager.ensureSufficientCredits()

        val token = authManager.currentAccessToken()
            ?: throw IllegalStateException("Not authenticated. Please sign in.")

        val body = buildProxyRequestBody(request, stream = false)

        val requestBuilder = Request.Builder()
            .url(proxyService.proxyBaseUrl)
            .post(body.toRequestBody("application/json".toMediaType()))
        proxyService.buildAuthHeaders(token).forEach { (k, v) ->
            requestBuilder.addHeader(k, v)
        }
        val httpRequest = requestBuilder.build()

        val response = withContext(Dispatchers.IO) {
            client.newCall(httpRequest).execute()
        }

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            when (response.code) {
                402 -> throw InsufficientCreditsException()
                401 -> throw IllegalStateException("Authentication expired. Please sign in again.")
                429 -> throw IllegalStateException("Rate limit exceeded. Please try again in a moment.")
                else -> throw IllegalStateException("Proxy error ${response.code}: $errorBody")
            }
        }

        val responseBody = response.body?.string()
            ?: throw IllegalStateException("Empty response from proxy")

        val parsed = parseCompletionResponse(responseBody)

        // Deduct credits locally (server already deducted atomically)
        if (parsed.usage != null) {
            val tier = when (AiMode.fromId(cellBreakConfig.aiMode)) {
                AiMode.FAST -> CreditCalculator.ModelTier.STANDARD
                AiMode.THINKING -> CreditCalculator.ModelTier.THINKING
            }
            val cost = CreditCalculator.calculateCostWithTier(
                parsed.usage.inputTokens,
                parsed.usage.outputTokens,
                tier
            )
            creditManager.deductLocally(cost)
        }

        return parsed
    }

    override fun stream(request: CompletionRequest): Flow<StreamEvent> = flow {
        creditManager.ensureSufficientCredits()

        val token = authManager.currentAccessToken()
            ?: throw IllegalStateException("Not authenticated. Please sign in.")

        val body = buildProxyRequestBody(request, stream = true)

        val requestBuilder = Request.Builder()
            .url(proxyService.proxyBaseUrl)
            .post(body.toRequestBody("application/json".toMediaType()))
        proxyService.buildAuthHeaders(token).forEach { (k, v) ->
            requestBuilder.addHeader(k, v)
        }
        val httpRequest = requestBuilder.build()

        val response = withContext(Dispatchers.IO) {
            client.newCall(httpRequest).execute()
        }

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            emit(StreamEvent.Error("Proxy error ${response.code}: $errorBody"))
            return@flow
        }

        val reader = response.body?.byteStream()?.bufferedReader()
            ?: run {
                emit(StreamEvent.Error("Empty stream response"))
                return@flow
            }

        var totalInputTokens = 0
        var totalOutputTokens = 0

        try {
            var line = reader.readLine()
            while (line != null) {
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data != "[DONE]") {
                        try {
                            val event = json.parseToJsonElement(data).jsonObject
                            val type = event["type"]?.jsonPrimitive?.contentOrNull

                            when (type) {
                                "text_delta" -> {
                                    val text = event["text"]?.jsonPrimitive?.contentOrNull ?: ""
                                    emit(StreamEvent.TextDelta(text))
                                }
                                "tool_use_start" -> {
                                    val id = event["id"]?.jsonPrimitive?.contentOrNull ?: ""
                                    val name = event["name"]?.jsonPrimitive?.contentOrNull ?: ""
                                    emit(StreamEvent.ToolUseStart(id, name))
                                }
                                "tool_use_input_delta" -> {
                                    val delta = event["delta"]?.jsonPrimitive?.contentOrNull ?: ""
                                    emit(StreamEvent.ToolUseInputDelta(delta))
                                }
                                "usage" -> {
                                    totalInputTokens = event["input_tokens"]?.jsonPrimitive?.intOrNull ?: 0
                                    totalOutputTokens = event["output_tokens"]?.jsonPrimitive?.intOrNull ?: 0
                                }
                                "complete" -> {
                                    val contentJson = event["content"]?.jsonArray
                                    val content = parseContentBlocks(contentJson ?: buildJsonArray { })
                                    val stopReason = parseStopReason(
                                        event["stop_reason"]?.jsonPrimitive?.contentOrNull
                                    )
                                    val usage = Usage(totalInputTokens, totalOutputTokens)
                                    emit(StreamEvent.Complete(
                                        CompletionResponse(content, stopReason, usage)
                                    ))
                                }
                                "error" -> {
                                    val message = event["message"]?.jsonPrimitive?.contentOrNull ?: "Unknown error"
                                    emit(StreamEvent.Error(message))
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse SSE event: $data", e)
                        }
                    }
                }
                line = reader.readLine()
            }
        } finally {
            reader.close()
            // Deduct credits locally based on accumulated usage
            if (totalInputTokens > 0 || totalOutputTokens > 0) {
                val streamTier = when (AiMode.fromId(cellBreakConfig.aiMode)) {
                    AiMode.FAST -> CreditCalculator.ModelTier.STANDARD
                    AiMode.THINKING -> CreditCalculator.ModelTier.THINKING
                }
                val cost = CreditCalculator.calculateCostWithTier(
                    totalInputTokens, totalOutputTokens, streamTier
                )
                creditManager.deductLocally(cost)
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun buildProxyRequestBody(request: CompletionRequest, stream: Boolean): String {
        return buildJsonObject {
            put("ai_mode", cellBreakConfig.aiMode)
            put("system_prompt", request.systemPrompt)
            putJsonArray("messages") {
                for (msg in request.messages) {
                    addJsonObject {
                        put("role", msg.role.name.lowercase())
                        putJsonArray("content") {
                            for (block in msg.content) {
                                addJsonObject {
                                    when (block) {
                                        is ContentBlock.Text -> {
                                            put("type", "text")
                                            put("text", block.text)
                                        }
                                        is ContentBlock.ToolUse -> {
                                            put("type", "tool_use")
                                            put("id", block.id)
                                            put("name", block.name)
                                            put("input", block.input)
                                        }
                                        is ContentBlock.ToolResult -> {
                                            put("type", "tool_result")
                                            put("tool_use_id", block.toolUseId)
                                            put("content", block.content)
                                            put("is_error", block.isError)
                                        }
                                        is ContentBlock.Image -> {
                                            put("type", "image")
                                            put("base64_data", block.base64Data)
                                            put("media_type", block.mediaType)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (request.tools.isNotEmpty()) {
                putJsonArray("tools") {
                    for (tool in request.tools) {
                        addJsonObject {
                            put("name", tool.name)
                            put("description", tool.description)
                            put("input_schema", json.encodeToJsonElement(tool.inputSchema))
                        }
                    }
                }
            }
            put("max_tokens", request.maxTokens)
            put("stream", stream)
        }.toString()
    }

    private fun parseCompletionResponse(body: String): CompletionResponse {
        val obj = json.parseToJsonElement(body).jsonObject
        val content = parseContentBlocks(obj["content"]?.jsonArray ?: buildJsonArray { })
        val stopReason = parseStopReason(obj["stop_reason"]?.jsonPrimitive?.contentOrNull)
        val usageObj = obj["usage"]?.jsonObject
        val usage = if (usageObj != null) {
            Usage(
                inputTokens = usageObj["input_tokens"]?.jsonPrimitive?.intOrNull ?: 0,
                outputTokens = usageObj["output_tokens"]?.jsonPrimitive?.intOrNull ?: 0
            )
        } else null
        return CompletionResponse(content, stopReason, usage)
    }

    private fun parseContentBlocks(array: JsonArray): List<ContentBlock> {
        return array.mapNotNull { element ->
            val obj = element.jsonObject
            when (obj["type"]?.jsonPrimitive?.contentOrNull) {
                "text" -> ContentBlock.Text(
                    text = obj["text"]?.jsonPrimitive?.contentOrNull ?: "",
                    thought = obj["thought"]?.jsonPrimitive?.booleanOrNull ?: false
                )
                "tool_use" -> ContentBlock.ToolUse(
                    id = obj["id"]?.jsonPrimitive?.contentOrNull ?: "",
                    name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "",
                    input = obj["input"]?.jsonObject ?: buildJsonObject { }
                )
                else -> null
            }
        }
    }

    private fun parseStopReason(reason: String?): StopReason {
        return when (reason) {
            "end_turn" -> StopReason.END_TURN
            "tool_use" -> StopReason.TOOL_USE
            "max_tokens" -> StopReason.MAX_TOKENS
            else -> StopReason.END_TURN
        }
    }

    companion object {
        private const val TAG = "ProxyProvider"
    }
}
