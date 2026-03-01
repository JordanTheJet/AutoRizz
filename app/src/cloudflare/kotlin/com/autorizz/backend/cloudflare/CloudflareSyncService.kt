package com.autorizz.backend.cloudflare

import android.util.Log
import com.autorizz.backend.SyncService
import com.autorizz.backend.SyncStatus
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

@Singleton
class CloudflareSyncService @Inject constructor(
    private val authService: CloudflareAuthService
) : SyncService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl = BuildConfig.CF_WORKER_URL

    private val _status = MutableStateFlow(SyncStatus.IDLE)
    override val status: StateFlow<SyncStatus> = _status.asStateFlow()

    override suspend fun pushChanges(userId: String, since: Long): Result<Unit> {
        _status.value = SyncStatus.SYNCING
        return try {
            // TODO: POST /sync/push with changed entities
            _status.value = SyncStatus.IDLE
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Push failed: ${e.message}")
            _status.value = SyncStatus.ERROR
            Result.failure(e)
        }
    }

    override suspend fun pullChanges(userId: String, since: Long): Result<Unit> {
        _status.value = SyncStatus.SYNCING
        return try {
            // TODO: GET /sync/pull?since=$since
            _status.value = SyncStatus.IDLE
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Pull failed: ${e.message}")
            _status.value = SyncStatus.ERROR
            Result.failure(e)
        }
    }

    override suspend fun subscribeToChanges(userId: String, onChanged: () -> Unit) {
        // Cloudflare Durable Objects support WebSockets for realtime
        // TODO: Connect to $baseUrl/sync/realtime via WebSocket
        Log.d(TAG, "Realtime subscription started for $userId")
    }

    override suspend fun unsubscribe() {
        _status.value = SyncStatus.IDLE
    }

    companion object {
        private const val TAG = "CloudflareSyncService"
    }
}
