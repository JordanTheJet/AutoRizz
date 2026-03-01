package com.autorizz.usage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autorizz.credits.CreditManager
import com.autorizz.credits.PurchaseResult
import com.autorizz.credits.PurchaseService
import com.autorizz.credits.SubscriptionPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PurchaseUiState(
    val isLoading: Boolean = false,
    val checkoutUrl: String? = null,
    val error: String? = null
)

@HiltViewModel
class UsageDashboardViewModel @Inject constructor(
    private val creditManager: CreditManager,
    private val purchaseService: PurchaseService
) : ViewModel() {

    val creditBalance: StateFlow<Long> = creditManager.balance

    private val _purchaseState = MutableStateFlow(PurchaseUiState())
    val purchaseState: StateFlow<PurchaseUiState> = _purchaseState.asStateFlow()

    fun subscribeToPlan(plan: SubscriptionPlan) {
        viewModelScope.launch {
            _purchaseState.value = PurchaseUiState(isLoading = true)
            when (val result = purchaseService.createSubscriptionCheckout(plan)) {
                is PurchaseResult.Success -> {
                    _purchaseState.value = PurchaseUiState(checkoutUrl = result.checkoutUrl)
                }
                is PurchaseResult.Error -> {
                    Log.e("UsageDashboard", "Subscribe failed: ${result.message}")
                    _purchaseState.value = PurchaseUiState(error = result.message)
                }
            }
        }
    }

    fun refreshAfterPurchase() {
        viewModelScope.launch {
            purchaseService.refreshAfterPurchase()
        }
    }

    fun clearPurchaseState() {
        _purchaseState.value = PurchaseUiState()
    }
}
