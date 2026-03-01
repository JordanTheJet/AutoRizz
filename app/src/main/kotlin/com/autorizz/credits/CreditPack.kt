package com.autorizz.credits

data class SubscriptionPlan(
    val id: String,
    val name: String,
    val monthlyCredits: Long,
    val priceUsd: Double,
    val polarProductId: String?,
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
    SubscriptionPlan("starter", "Starter", 1_000, 4.99, "d6ca4d91-9857-41ba-bb9f-036c305ca35e", listOf(AiMode.FAST, AiMode.THINKING)),
    SubscriptionPlan("pro", "Pro", 10_000, 19.99, "b1d359a5-1af9-43df-860d-328d9633e6c3", listOf(AiMode.FAST, AiMode.THINKING)),
    SubscriptionPlan("ultra", "Ultra", 100_000, 99.00, "31f47752-8f15-4c37-9b98-2b223bbd5569", listOf(AiMode.FAST, AiMode.THINKING))
)

fun planForId(id: String): SubscriptionPlan =
    SUBSCRIPTION_PLANS.firstOrNull { it.id == id } ?: SUBSCRIPTION_PLANS[0]

const val WELCOME_BONUS_CREDITS = 100L
