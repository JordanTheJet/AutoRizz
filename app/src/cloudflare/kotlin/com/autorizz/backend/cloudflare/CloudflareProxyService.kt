package com.autorizz.backend.cloudflare

import com.autorizz.backend.ProxyService
import com.cellclaw.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudflareProxyService @Inject constructor() : ProxyService {

    override val proxyBaseUrl: String = "${BuildConfig.CF_WORKER_URL}/llm-proxy"

    override fun buildAuthHeaders(token: String): Map<String, String> {
        return mapOf(
            "Authorization" to "Bearer $token",
            "Content-Type" to "application/json"
        )
    }
}
