package com.autorizz.credits

data class SubscriptionPlan(
    val id: String,
    val name: String,
    val monthlyCredits: Long,
    val priceUsd: Double,
    val stripePriceId: String?,
    val aiModes: List<AiMode>
) {
    val priceDisplay: String
        get() = if (priceUsd == 0.0) "Free" else "$${String.format("%.2f", priceUsd)}/mo"

    val creditsDisplay: String
        get() = when {
            monthlyCredits >= 100_000 -> "${monthlyCredits / 1_000}K"
            monthlyCredits >= 1_000 -> "${String.format("%.1f", monthlyCredits / 1_000.0)}K"
            else -> monthlyCredits.toString()
        }
}

enum class AiMode(val id: String, val displayName: String, val description: String) {
    FAST("fast", "Fast", "Quick responses, simple tasks"),
    THINKING("thinking", "Thinking", "Deep reasoning, complex tasks");

    companion object {
        fun fromId(id: String): AiMode = entries.firstOrNull { it.id == id } ?: FAST
    }
}

val SUBSCRIPTION_PLANS = listOf(
    SubscriptionPlan("free", "Free", 100, 0.0, null, listOf(AiMode.FAST, AiMode.THINKING)),
    SubscriptionPlan("starter", "Starter", 1_000, 4.99, "price_1T5tVfQWWPsMEhJU15QWTGXI", listOf(AiMode.FAST, AiMode.THINKING)),
    SubscriptionPlan("pro", "Pro", 10_000, 19.99, "price_1T5tVmQWWPsMEhJUC8ntvGR9", listOf(AiMode.FAST, AiMode.THINKING)),
    SubscriptionPlan("ultra", "Ultra", 100_000, 99.00, "price_1T5tVoQWWPsMEhJUvhyRgsVV", listOf(AiMode.FAST, AiMode.THINKING))
)

fun planForId(id: String): SubscriptionPlan =
    SUBSCRIPTION_PLANS.firstOrNull { it.id == id } ?: SUBSCRIPTION_PLANS[0]

const val WELCOME_BONUS_CREDITS = 100L
