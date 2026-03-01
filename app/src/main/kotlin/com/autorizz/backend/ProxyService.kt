package com.autorizz.backend

/**
 * Backend-agnostic LLM proxy configuration.
 * The proxy is a separate concern from the database — you could use
 * Cloudflare Workers for the proxy with PocketBase for data.
 */
interface ProxyService {
    /** Base URL for the LLM proxy endpoint */
    val proxyBaseUrl: String

    /**
     * Build the authorization headers for proxy requests.
     * Supabase: Bearer <supabase-jwt>
     * Cloudflare: Bearer <cf-jwt>
     * PocketBase: Bearer <pb-token>
     */
    fun buildAuthHeaders(token: String): Map<String, String>
}
