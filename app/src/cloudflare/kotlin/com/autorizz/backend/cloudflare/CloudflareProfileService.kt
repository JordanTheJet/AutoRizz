package com.autorizz.backend.cloudflare

import android.util.Log
import com.autorizz.backend.ProfileService
import com.autorizz.backend.model.ProfileUpdates
import com.autorizz.backend.model.UserProfileData
import com.cellclaw.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudflareProfileService @Inject constructor(
    private val authService: CloudflareAuthService
) : ProfileService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val baseUrl = BuildConfig.CF_WORKER_URL

    override suspend fun getProfile(userId: String): Result<UserProfileData> {
        return try {
            val token = authService.currentAccessToken()
                ?: return Result.failure(IllegalStateException("Not authenticated"))

            val request = Request.Builder()
                .url("$baseUrl/profile")
                .addHeader("Authorization", "Bearer $token")
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            if (!response.isSuccessful) {
                return Result.failure(IllegalStateException("Failed to get profile"))
            }

            val body = response.body?.string()
                ?: return Result.failure(IllegalStateException("Empty response"))
            val parsed = json.parseToJsonElement(body).jsonObject

            Result.success(
                UserProfileData(
                    id = parsed["id"]?.jsonPrimitive?.contentOrNull ?: userId,
                    email = parsed["email"]?.jsonPrimitive?.contentOrNull ?: "",
                    displayName = parsed["display_name"]?.jsonPrimitive?.contentOrNull,
                    creditBalance = parsed["credit_balance"]?.jsonPrimitive?.long ?: 0L,
                    referralCode = parsed["referral_code"]?.jsonPrimitive?.contentOrNull,
                    createdAt = parsed["created_at"]?.jsonPrimitive?.long ?: 0L
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get profile: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(userId: String, updates: ProfileUpdates): Result<Unit> {
        return try {
            val token = authService.currentAccessToken()
                ?: return Result.failure(IllegalStateException("Not authenticated"))

            val body = buildJsonObject {
                updates.displayName?.let { put("display_name", it) }
                updates.autoRefillEnabled?.let { put("auto_refill_enabled", it) }
                updates.autoRefillPack?.let { put("auto_refill_pack", it) }
                updates.autoRefillThreshold?.let { put("auto_refill_threshold", it) }
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/profile")
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .patch(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            if (!response.isSuccessful) {
                return Result.failure(IllegalStateException("Failed to update profile"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile: ${e.message}")
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "CloudflareProfileService"
    }
}
