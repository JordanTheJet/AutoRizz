package com.autorizz.backend.supabase

import android.util.Log
import com.autorizz.backend.CreditService
import com.autorizz.credits.SUBSCRIPTION_PLANS
import com.autorizz.credits.SubscriptionPlan
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseCreditService @Inject constructor(
    private val supabase: SupabaseClientProvider
) : CreditService {

    override suspend fun getBalance(userId: String): Result<Long> {
        return try {
            val result = supabase.client.postgrest
                .from("user_profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingle<Map<String, kotlinx.serialization.json.JsonElement>>()
            val balance = result["credit_balance"]?.jsonPrimitive?.long ?: 0L
            Result.success(balance)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get balance: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun refreshBalance(userId: String): Result<Long> {
        return getBalance(userId)
    }

    override suspend fun getSubscriptionPlans(): Result<List<SubscriptionPlan>> {
        return Result.success(SUBSCRIPTION_PLANS)
    }

    companion object {
        private const val TAG = "SupabaseCreditService"
    }
}
