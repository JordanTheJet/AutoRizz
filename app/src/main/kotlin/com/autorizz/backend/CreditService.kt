package com.autorizz.backend

import com.autorizz.credits.SubscriptionPlan

/**
 * Backend-agnostic credit balance operations.
 * Server-side deduction happens atomically in the proxy; this is the client view.
 */
interface CreditService {
    suspend fun getBalance(userId: String): Result<Long>
    suspend fun refreshBalance(userId: String): Result<Long>
    suspend fun getSubscriptionPlans(): Result<List<SubscriptionPlan>>
}
