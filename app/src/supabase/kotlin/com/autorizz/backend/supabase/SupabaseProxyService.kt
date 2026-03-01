package com.autorizz.backend.supabase

import com.autorizz.backend.ProxyService
import com.cellclaw.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseProxyService @Inject constructor() : ProxyService {

    override val proxyBaseUrl: String =
        "${BuildConfig.SUPABASE_URL}/functions/v1/llm-proxy"

    override fun buildAuthHeaders(token: String): Map<String, String> {
        return mapOf(
            "Authorization" to "Bearer $token",
            "Content-Type" to "application/json"
        )
    }
}
