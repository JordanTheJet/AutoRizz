package com.cellclaw.provider

/**
 * Contract for managing AI providers. Extracted from ProviderManager
 * so AutoRizz can swap in AutoRizzProviderManager via Hilt.
 */
interface ProviderManagerContract {
    fun activeProvider(): Provider
    fun switchProvider(type: String)
    fun activeType(): String
    fun availableProviders(): List<ProviderInfo>
    fun hasKey(type: String): Boolean
    fun setApiKey(type: String, apiKey: String)
    fun removeApiKey(type: String)

    /** Complete a request with automatic cross-provider failover. */
    suspend fun completeWithFailover(request: CompletionRequest): CompletionResponse

    /** Last failover event for UI visibility, null if no failover occurred. */
    val lastFailoverEvent: FailoverEvent?
}
