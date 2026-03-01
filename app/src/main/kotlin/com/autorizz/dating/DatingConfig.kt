package com.autorizz.dating

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatingConfig @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("autorizz_dating_config", Context.MODE_PRIVATE)

    var enabledApps: Set<String>
        get() = prefs.getStringSet("enabled_apps", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("enabled_apps", value).apply()

    var conversationStyle: String
        get() = prefs.getString("conversation_style", "casual") ?: "casual"
        set(value) = prefs.edit().putString("conversation_style", value).apply()

    var minReplyDelay: Int
        get() = prefs.getInt("min_reply_delay", 5)
        set(value) = prefs.edit().putInt("min_reply_delay", value).apply()

    var maxReplyDelay: Int
        get() = prefs.getInt("max_reply_delay", 30)
        set(value) = prefs.edit().putInt("max_reply_delay", value).apply()

    var activeHoursStart: Int
        get() = prefs.getInt("active_hours_start", 8)
        set(value) = prefs.edit().putInt("active_hours_start", value).apply()

    var activeHoursEnd: Int
        get() = prefs.getInt("active_hours_end", 23)
        set(value) = prefs.edit().putInt("active_hours_end", value).apply()

    var autoMessageMatches: Boolean
        get() = prefs.getBoolean("auto_message_matches", true)
        set(value) = prefs.edit().putBoolean("auto_message_matches", value).apply()

    var dateApprovalRequired: Boolean
        get() = prefs.getBoolean("date_approval_required", true)
        set(value) = prefs.edit().putBoolean("date_approval_required", value).apply()

    var swipeModel: String?
        get() = prefs.getString("swipe_model", null)
        set(value) = prefs.edit().putString("swipe_model", value).apply()

    var conversationModel: String?
        get() = prefs.getString("conversation_model", null)
        set(value) = prefs.edit().putString("conversation_model", value).apply()

    var datingOnboardingComplete: Boolean
        get() = prefs.getBoolean("dating_onboarding_complete", false)
        set(value) = prefs.edit().putBoolean("dating_onboarding_complete", value).apply()

    fun isAppEnabled(app: String): Boolean = app in enabledApps

    fun toggleApp(app: String, enabled: Boolean) {
        val current = enabledApps.toMutableSet()
        if (enabled) current.add(app) else current.remove(app)
        enabledApps = current
    }

    companion object {
        const val APP_HINGE = "hinge"
        const val APP_TINDER = "tinder"
        const val APP_BUMBLE = "bumble"

        val ALL_APPS = listOf(APP_HINGE, APP_TINDER, APP_BUMBLE)

        val APP_PACKAGES = mapOf(
            APP_HINGE to "co.hinge.app",
            APP_TINDER to "com.tinder",
            APP_BUMBLE to "com.bumble.app"
        )

        val CONVERSATION_STYLES = listOf("casual", "flirty", "direct", "funny")
    }
}
