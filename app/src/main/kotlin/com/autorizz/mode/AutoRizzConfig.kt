package com.autorizz.mode

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoRizzConfig @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("autorizz_config", Context.MODE_PRIVATE)

    var userMode: String
        get() = prefs.getString("user_mode", "PRO") ?: "PRO"
        set(value) = prefs.edit().putString("user_mode", value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("is_logged_in", false)
        set(value) = prefs.edit().putBoolean("is_logged_in", value).apply()

    var cachedCreditBalance: Long
        get() = prefs.getLong("credit_balance", 0L)
        set(value) = prefs.edit().putLong("credit_balance", value).apply()

    var userId: String?
        get() = prefs.getString("user_id", null)
        set(value) = prefs.edit().putString("user_id", value).apply()

    var userEmail: String?
        get() = prefs.getString("user_email", null)
        set(value) = prefs.edit().putString("user_email", value).apply()

    var onboardingComplete: Boolean
        get() = prefs.getBoolean("onboarding_complete", false)
        set(value) = prefs.edit().putBoolean("onboarding_complete", value).apply()

    var accessToken: String?
        get() = prefs.getString("access_token", null)
        set(value) = prefs.edit().putString("access_token", value).apply()

    var refreshToken: String?
        get() = prefs.getString("refresh_token", null)
        set(value) = prefs.edit().putString("refresh_token", value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong("last_sync_timestamp", 0L)
        set(value) = prefs.edit().putLong("last_sync_timestamp", value).apply()

    var autoRefillEnabled: Boolean
        get() = prefs.getBoolean("auto_refill_enabled", false)
        set(value) = prefs.edit().putBoolean("auto_refill_enabled", value).apply()

    var autoRefillPack: String?
        get() = prefs.getString("auto_refill_pack", null)
        set(value) = prefs.edit().putString("auto_refill_pack", value).apply()

    var autoRefillThreshold: Long
        get() = prefs.getLong("auto_refill_threshold", 50L)
        set(value) = prefs.edit().putLong("auto_refill_threshold", value).apply()

    var aiMode: String
        get() = prefs.getString("ai_mode", "fast") ?: "fast"
        set(value) = prefs.edit().putString("ai_mode", value).apply()

    var subscriptionPlan: String
        get() = prefs.getString("subscription_plan", "free") ?: "free"
        set(value) = prefs.edit().putString("subscription_plan", value).apply()

    var subscriptionStatus: String
        get() = prefs.getString("subscription_status", "active") ?: "active"
        set(value) = prefs.edit().putString("subscription_status", value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
