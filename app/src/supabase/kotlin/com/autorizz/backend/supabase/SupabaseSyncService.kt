package com.autorizz.backend.supabase

import android.util.Log
import com.autorizz.backend.SyncService
import com.autorizz.backend.SyncStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseSyncService @Inject constructor(
    private val supabase: SupabaseClientProvider
) : SyncService {

    private val _status = MutableStateFlow(SyncStatus.IDLE)
    override val status: StateFlow<SyncStatus> = _status.asStateFlow()

    override suspend fun pushChanges(userId: String, since: Long): Result<Unit> {
        _status.value = SyncStatus.SYNCING
        return try {
            // TODO: Push local Room entities to Supabase postgrest
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
            // TODO: Pull changes from Supabase postgrest since timestamp
            _status.value = SyncStatus.IDLE
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Pull failed: ${e.message}")
            _status.value = SyncStatus.ERROR
            Result.failure(e)
        }
    }

    override suspend fun subscribeToChanges(userId: String, onChanged: () -> Unit) {
        // TODO: Subscribe to Supabase realtime changes
        Log.d(TAG, "Realtime subscription started for $userId")
    }

    override suspend fun unsubscribe() {
        try {
            supabase.client.realtime.removeAllChannels()
        } catch (_: Exception) {}
        _status.value = SyncStatus.IDLE
    }

    companion object {
        private const val TAG = "SupabaseSyncService"
    }
}
