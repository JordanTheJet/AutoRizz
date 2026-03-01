package com.autorizz.version

import android.util.Log
import com.cellclaw.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class VersionInfo(
    val min_version_code: Int,
    val latest_version_code: Int,
    val latest_version_name: String,
    val update_url: String,
    val force_update: Boolean = false,
    val update_available: Boolean = false,
    val update_message: String? = null
)

sealed class VersionStatus {
    data object Current : VersionStatus()
    data class UpdateAvailable(val info: VersionInfo) : VersionStatus()
    data class ForceUpdate(val info: VersionInfo) : VersionStatus()
    data class Error(val message: String) : VersionStatus()
}

@Singleton
class VersionCheck @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun check(): VersionStatus = withContext(Dispatchers.IO) {
        try {
            val url = "${BuildConfig.SUPABASE_URL}/functions/v1/version-check?v=${BuildConfig.VERSION_CODE}"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext VersionStatus.Error("HTTP ${response.code}")
            }

            val body = response.body?.string()
                ?: return@withContext VersionStatus.Error("Empty response")

            val info = json.decodeFromString<VersionInfo>(body)

            when {
                info.force_update -> VersionStatus.ForceUpdate(info)
                info.update_available -> VersionStatus.UpdateAvailable(info)
                else -> VersionStatus.Current
            }
        } catch (e: Exception) {
            Log.w(TAG, "Version check failed: ${e.message}")
            VersionStatus.Error(e.message ?: "Unknown error")
        }
    }

    companion object {
        private const val TAG = "VersionCheck"
    }
}
