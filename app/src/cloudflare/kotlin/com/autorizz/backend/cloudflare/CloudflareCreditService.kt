package com.autorizz.backend.cloudflare

import android.util.Log
import com.autorizz.backend.CreditService
import com.cellclaw.BuildConfig
import com.autorizz.credits.SUBSCRIPTION_PLANS
import com.autorizz.credits.SubscriptionPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudflareCreditService @Inject constructor(
    private val authService: CloudflareAuthService
) : CreditService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val baseUrl = BuildConfig.CF_WORKER_URL

    override suspend fun getBalance(userId: String): Result<Long> {
        return try {
            val token = authService.currentAccessToken()
                ?: return Result.failure(IllegalStateException("Not authenticated"))
            val request = Request.Builder()
                .url("$baseUrl/credits/balance")
                .addHeader("Authorization", "Bearer $token")
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            if (!response.isSuccessful) {
                return Result.failure(IllegalStateException("Failed to get balance"))
            }

            val body = response.body?.string() ?: return Result.failure(IllegalStateException("Empty response"))
            val parsed = json.parseToJsonElement(body).jsonObject
            val balance = parsed["balance"]?.jsonPrimitive?.long ?: 0L
            Result.success(balance)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get balance: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun refreshBalance(userId: String): Result<Long> = getBalance(userId)

    override suspend fun getSubscriptionPlans(): Result<List<SubscriptionPlan>> {
        return Result.success(SUBSCRIPTION_PLANS)
    }

    companion object {
        private const val TAG = "CloudflareCreditService"
    }
}
