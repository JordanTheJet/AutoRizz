package com.autorizz.backend

import kotlinx.coroutines.flow.StateFlow

enum class SyncStatus {
    IDLE, SYNCING, ERROR
}

/**
 * Backend-agnostic data sync operations.
 * Handles pushing/pulling conversations, messages, and memory between device and server.
 */
interface SyncService {
    val status: StateFlow<SyncStatus>

    suspend fun pushChanges(userId: String, since: Long): Result<Unit>
    suspend fun pullChanges(userId: String, since: Long): Result<Unit>
    suspend fun subscribeToChanges(userId: String, onChanged: () -> Unit)
    suspend fun unsubscribe()
}
