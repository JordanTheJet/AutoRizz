package com.autorizz.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autorizz.backend.AuthState
import com.autorizz.backend.CreditService
import com.autorizz.credits.CreditManager
import com.autorizz.credits.WELCOME_BONUS_CREDITS
import com.autorizz.mode.AutoRizzConfig
import com.autorizz.mode.Mode
import com.autorizz.mode.ModeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val modeManager: ModeManager,
    private val creditManager: CreditManager,
    private val creditService: CreditService,
    private val cellBreakConfig: AutoRizzConfig
) : ViewModel() {

    val authState: StateFlow<AuthState> = authManager.authState

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            val result = authManager.signInWithEmail(email, password)
            if (result.isSuccess) {
                modeManager.switchMode(Mode.PRO)
                refreshCredits()
            }
        }
    }

    private suspend fun refreshCredits() {
        val userId = cellBreakConfig.userId ?: return
        creditService.getBalance(userId)
            .onSuccess { creditManager.setBalance(it) }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            val result = authManager.signUpWithEmail(email, password)
            if (result.isSuccess) {
                creditManager.setBalance(WELCOME_BONUS_CREDITS)
                cellBreakConfig.subscriptionPlan = "free"
                modeManager.switchMode(Mode.PRO)
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _resetState.value = ResetState.Loading
            val result = authManager.resetPassword(email)
            _resetState.value = if (result.isSuccess) {
                ResetState.Sent
            } else {
                ResetState.Error(result.exceptionOrNull()?.message ?: "Failed to send reset email")
            }
        }
    }

    private val _resetState = MutableStateFlow<ResetState>(ResetState.Idle)
    val resetState: StateFlow<ResetState> = _resetState

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
        }
    }
}

sealed class ResetState {
    data object Idle : ResetState()
    data object Loading : ResetState()
    data object Sent : ResetState()
    data class Error(val message: String) : ResetState()
}
