package com.autorizz.backend

import kotlinx.coroutines.flow.StateFlow

sealed class AuthState {
    data object SignedOut : AuthState()
    data object Loading : AuthState()
    data class SignedIn(val user: UserInfo) : AuthState()
    data class Error(val message: String) : AuthState()
}

data class UserInfo(
    val id: String,
    val email: String,
    val displayName: String?
)

/**
 * Backend-agnostic authentication service.
 * Each backend (Supabase, Cloudflare, PocketBase) provides its own implementation.
 */
interface AuthService {
    val authState: StateFlow<AuthState>
    val isSignedIn: Boolean

    suspend fun signInWithEmail(email: String, password: String): Result<Unit>
    suspend fun signUpWithEmail(email: String, password: String): Result<Unit>
    suspend fun signOut()
    suspend fun refreshSession(): Boolean
    suspend fun resetPassword(email: String): Result<Unit>
    fun currentAccessToken(): String?
    fun currentUser(): UserInfo?
}
