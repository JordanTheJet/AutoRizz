package com.autorizz.backend.supabase

import android.util.Log
import com.autorizz.backend.AuthService
import com.autorizz.backend.AuthState
import com.autorizz.backend.UserInfo
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthService @Inject constructor(
    private val supabase: SupabaseClientProvider
) : AuthService {
    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override val isSignedIn: Boolean get() = _authState.value is AuthState.SignedIn

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        _authState.value = AuthState.Loading
        return try {
            supabase.client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            updateAuthState()
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
            supabase.client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            updateAuthState()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed: ${e.message}")
            _authState.value = AuthState.Error(e.message ?: "Sign up failed")
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        try {
            supabase.client.auth.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Sign out error: ${e.message}")
        }
        _authState.value = AuthState.SignedOut
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            supabase.client.auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Password reset failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun refreshSession(): Boolean {
        return try {
            supabase.client.auth.refreshCurrentSession()
            updateAuthState()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Session refresh failed: ${e.message}")
            false
        }
    }

    override fun currentAccessToken(): String? {
        return try {
            supabase.client.auth.currentAccessTokenOrNull()
        } catch (_: Exception) {
            null
        }
    }

    override fun currentUser(): UserInfo? {
        return (_authState.value as? AuthState.SignedIn)?.user
    }

    private fun updateAuthState() {
        val session = supabase.client.auth.currentSessionOrNull()
        if (session != null) {
            val user = supabase.client.auth.currentUserOrNull()
            val profile = UserInfo(
                id = user?.id ?: "",
                email = user?.email ?: "",
                displayName = null
            )
            _authState.value = AuthState.SignedIn(profile)
        } else {
            _authState.value = AuthState.SignedOut
        }
    }

    companion object {
        private const val TAG = "SupabaseAuthService"
    }
}
