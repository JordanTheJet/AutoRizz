package com.autorizz.backend.cloudflare

import android.util.Log
import com.autorizz.backend.AuthService
import com.autorizz.backend.AuthState
import com.autorizz.backend.UserInfo
import com.cellclaw.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * Auth via self-rolled JWT endpoints on Cloudflare Worker.
 * The Worker handles password hashing and JWT signing/verification.
 */
@Singleton
class CloudflareAuthService @Inject constructor() : AuthService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val baseUrl = BuildConfig.CF_WORKER_URL

    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override val isSignedIn: Boolean get() = _authState.value is AuthState.SignedIn

    private var _accessToken: String? = null
    private var _refreshToken: String? = null

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        _authState.value = AuthState.Loading
        return try {
            val body = buildJsonObject {
                put("email", email)
                put("password", password)
            }.toString()

            val response = postJson("$baseUrl/auth/signin", body)
            val parsed = json.parseToJsonElement(response).jsonObject
            handleAuthResponse(parsed)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed: ${e.message}")
            _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<Unit> {
        _authState.value = AuthState.Loading
        return try {
            val body = buildJsonObject {
                put("email", email)
                put("password", password)
            }.toString()

            val response = postJson("$baseUrl/auth/signup", body)
            val parsed = json.parseToJsonElement(response).jsonObject
            handleAuthResponse(parsed)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed: ${e.message}")
            _authState.value = AuthState.Error(e.message ?: "Sign up failed")
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        _accessToken = null
        _refreshToken = null
        _authState.value = AuthState.SignedOut
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            val body = buildJsonObject {
                put("email", email)
            }.toString()
            postJson("$baseUrl/auth/reset-password", body)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Password reset failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun refreshSession(): Boolean {
        val token = _refreshToken ?: return false
        return try {
            val body = buildJsonObject {
                put("refresh_token", token)
            }.toString()

            val response = postJson("$baseUrl/auth/refresh", body)
            val parsed = json.parseToJsonElement(response).jsonObject
            handleAuthResponse(parsed)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Session refresh failed: ${e.message}")
            false
        }
    }

    override fun currentAccessToken(): String? = _accessToken

    override fun currentUser(): UserInfo? {
        return (_authState.value as? AuthState.SignedIn)?.user
    }

    private fun handleAuthResponse(response: JsonObject) {
        val token = response["access_token"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalStateException("No access_token in response")
        val refresh = response["refresh_token"]?.jsonPrimitive?.contentOrNull
        val user = response["user"]?.jsonObject

        _accessToken = token
        _refreshToken = refresh

        val userInfo = UserInfo(
            id = user?.get("id")?.jsonPrimitive?.contentOrNull ?: "",
            email = user?.get("email")?.jsonPrimitive?.contentOrNull ?: "",
            displayName = user?.get("display_name")?.jsonPrimitive?.contentOrNull
        )
        _authState.value = AuthState.SignedIn(userInfo)
    }

    private suspend fun postJson(url: String, body: String): String {
        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        return withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                throw IllegalStateException("Auth error ${response.code}: $errorBody")
            }
            response.body?.string() ?: throw IllegalStateException("Empty response")
        }
    }

    companion object {
        private const val TAG = "CloudflareAuthService"
    }
}
