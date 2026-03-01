package com.autorizz.credits

import android.util.Log
import com.autorizz.auth.AuthManager
import com.cellclaw.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class CheckoutResponse(
    val checkout_url: String,
    val session_id: String
)

sealed class PurchaseResult {
    data class Success(val checkoutUrl: String, val sessionId: String) : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
}

@Singleton
class PurchaseService @Inject constructor(
    private val authManager: AuthManager,
    private val creditManager: CreditManager
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Creates a Polar Checkout Session for a subscription plan.
     * Returns a URL to open in the browser.
     */
    suspend fun createSubscriptionCheckout(plan: SubscriptionPlan): PurchaseResult = withContext(Dispatchers.IO) {
        try {
            if (plan.id == "free") {
                return@withContext PurchaseResult.Error("Free plan doesn't require checkout")
            }

            val token = authManager.currentAccessToken()
                ?: return@withContext PurchaseResult.Error("Not signed in")

            val body = """{"plan_id":"${plan.id}"}"""
            val request = Request.Builder()
                .url("${BuildConfig.SUPABASE_URL}/functions/v1/polar-checkout")
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
                ?: return@withContext PurchaseResult.Error("Empty response")

            if (!response.isSuccessful) {
                Log.e(TAG, "Checkout failed: ${response.code} $responseBody")
                return@withContext PurchaseResult.Error("Checkout failed: ${response.code}")
            }

            val checkout = json.decodeFromString<CheckoutResponse>(responseBody)
            PurchaseResult.Success(checkout.checkout_url, checkout.session_id)
        } catch (e: Exception) {
            Log.e(TAG, "Checkout error: ${e.message}", e)
            PurchaseResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Called after returning from Polar checkout to refresh the credit balance.
     */
    suspend fun refreshAfterPurchase() {
        val userId = authManager.currentUser()?.id ?: return
        withContext(Dispatchers.IO) {
            repeat(3) { attempt ->
                try {
                    val token = authManager.currentAccessToken() ?: return@withContext
                    val request = Request.Builder()
                        .url("${BuildConfig.SUPABASE_URL}/rest/v1/user_profiles?select=credit_balance,subscription_plan,subscription_status&id=eq.$userId")
                        .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer $token")
                        .get()
                        .build()

                    val response = client.newCall(request).execute()
                    val body = response.body?.string() ?: return@withContext

                    val balanceMatch = Regex(""""credit_balance"\s*:\s*(\d+)""").find(body)
                    val balance = balanceMatch?.groupValues?.get(1)?.toLongOrNull()
                    if (balance != null && balance > creditManager.balance.value) {
                        creditManager.setBalance(balance)
                        Log.d(TAG, "Balance refreshed after purchase: $balance")
                        return@withContext
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Refresh attempt ${attempt + 1} failed: ${e.message}")
                }
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    companion object {
        private const val TAG = "PurchaseService"
    }
}
