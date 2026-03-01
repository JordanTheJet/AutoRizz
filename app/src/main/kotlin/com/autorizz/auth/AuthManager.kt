package com.autorizz.auth

import com.autorizz.backend.AuthService
import com.autorizz.backend.AuthState
import com.autorizz.backend.UserInfo
import com.autorizz.mode.AutoRizzConfig
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val authService: AuthService,
    private val cellBreakConfig: AutoRizzConfig
) {
    val authState: StateFlow<AuthState> = authService.authState

    val isSignedIn: Boolean get() = authService.isSignedIn

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        val result = authService.signInWithEmail(email, password)
        if (result.isSuccess) updateConfig(authService.currentUser())
        return result
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> {
        val result = authService.signUpWithEmail(email, password)
        if (result.isSuccess) updateConfig(authService.currentUser())
        return result
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return authService.resetPassword(email)
    }

    suspend fun signOut() {
        authService.signOut()
        cellBreakConfig.isLoggedIn = false
        cellBreakConfig.userId = null
        cellBreakConfig.userEmail = null
        cellBreakConfig.accessToken = null
        cellBreakConfig.refreshToken = null
    }

    suspend fun refreshSession(): Boolean {
        val success = authService.refreshSession()
        if (success) updateConfig(authService.currentUser())
        return success
    }

    fun currentAccessToken(): String? = authService.currentAccessToken()

    fun currentUser(): UserInfo? = authService.currentUser()

    private fun updateConfig(user: UserInfo?) {
        if (user != null) {
            cellBreakConfig.isLoggedIn = true
            cellBreakConfig.userId = user.id
            cellBreakConfig.userEmail = user.email
        } else {
            cellBreakConfig.isLoggedIn = false
        }
    }

    companion object {
        private const val TAG = "AuthManager"
    }
}
