package com.autorizz.sync

import android.util.Log
import com.autorizz.auth.AuthManager
import com.autorizz.backend.SyncService
import com.autorizz.backend.SyncStatus
import com.autorizz.mode.AutoRizzConfig
import com.autorizz.mode.Mode
import com.autorizz.mode.ModeManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncEngine @Inject constructor(
    private val modeManager: ModeManager,
    private val authManager: AuthManager,
    private val syncService: SyncService,
    private val cellBreakConfig: AutoRizzConfig
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val status: StateFlow<SyncStatus> = syncService.status

    private val _lastSyncTime = MutableStateFlow(cellBreakConfig.lastSyncTimestamp)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private var syncJob: Job? = null

    fun start() {
        syncJob?.cancel()
        syncJob = scope.launch {
            modeManager.modeFlow.collect { mode ->
                when (mode) {
                    Mode.PRO -> {
                        if (authManager.isSignedIn) {
                            startSync()
                        }
                    }
                    Mode.BYOK -> {
                        stopSync()
                    }
                }
            }
        }
    }

    fun stop() {
        syncJob?.cancel()
        stopSync()
    }

    suspend fun forcePush() {
        if (!authManager.isSignedIn) return
        val userId = authManager.currentUser()?.id ?: return
        syncService.pushChanges(userId, cellBreakConfig.lastSyncTimestamp)
        updateLastSyncTime()
    }

    suspend fun forcePull() {
        if (!authManager.isSignedIn) return
        val userId = authManager.currentUser()?.id ?: return
        syncService.pullChanges(userId, cellBreakConfig.lastSyncTimestamp)
        updateLastSyncTime()
    }

    private suspend fun startSync() {
        Log.d(TAG, "Starting sync engine")
        try {
            forcePull()
            val userId = authManager.currentUser()?.id ?: return
            syncService.subscribeToChanges(userId) {
                scope.launch { forcePull() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync start failed: ${e.message}")
        }
    }

    private fun stopSync() {
        Log.d(TAG, "Stopping sync engine")
        scope.launch {
            syncService.unsubscribe()
        }
    }

    private fun updateLastSyncTime() {
        val now = System.currentTimeMillis()
        _lastSyncTime.value = now
        cellBreakConfig.lastSyncTimestamp = now
    }

    companion object {
        private const val TAG = "SyncEngine"
    }
}
