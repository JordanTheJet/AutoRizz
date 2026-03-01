package com.autorizz.credits

import android.util.Log
import com.autorizz.mode.AutoRizzConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

class InsufficientCreditsException : Exception("Insufficient credits. Buy more in the Usage dashboard.")

@Singleton
class CreditManager @Inject constructor(
    private val cellBreakConfig: AutoRizzConfig
) {
    private val _balance = MutableStateFlow(cellBreakConfig.cachedCreditBalance)
    val balance: StateFlow<Long> = _balance.asStateFlow()

    fun ensureSufficientCredits() {
        if (_balance.value <= 0) throw InsufficientCreditsException()
    }

    fun deductLocally(amount: Long) {
        val newBalance = (_balance.value - amount).coerceAtLeast(0)
        _balance.value = newBalance
        cellBreakConfig.cachedCreditBalance = newBalance
        Log.d(TAG, "Deducted $amount credits. Balance: $newBalance")
    }

    fun addCreditsLocally(amount: Long) {
        val newBalance = _balance.value + amount
        _balance.value = newBalance
        cellBreakConfig.cachedCreditBalance = newBalance
        Log.d(TAG, "Added $amount credits. Balance: $newBalance")
    }

    fun setBalance(balance: Long) {
        _balance.value = balance
        cellBreakConfig.cachedCreditBalance = balance
    }

    fun isLowBalance(): Boolean = _balance.value in 1..lowBalanceThreshold()
    fun isZeroBalance(): Boolean = _balance.value <= 0

    private fun lowBalanceThreshold(): Long {
        // 10% of the starter pack
        return 50L
    }

    companion object {
        private const val TAG = "CreditManager"
    }
}
