package com.autorizz.credits

import kotlin.math.ceil
import kotlin.math.max

/**
 * Maps token usage to credit cost based on model tier.
 *
 * Credit system:
 * - 1 credit ≈ $0.01 of AI usage
 * - Internally: (inputTokens + outputTokens × 3) × tierMultiplier / 1000
 * - Minimum 1 credit per request
 */
object CreditCalculator {

    enum class ModelTier(val multiplier: Int, val displayName: String) {
        STANDARD(1, "Standard"),
        THINKING(5, "Thinking")
    }

    fun calculateCost(inputTokens: Int, outputTokens: Int, model: String): Long {
        val tier = tierForModel(model)
        return calculateCostWithTier(inputTokens, outputTokens, tier)
    }

    fun calculateCostWithTier(inputTokens: Int, outputTokens: Int, tier: ModelTier): Long {
        val rawCost = (inputTokens.toLong() * tier.multiplier) +
                (outputTokens.toLong() * 3 * tier.multiplier)
        return max(1L, ceil(rawCost / 1000.0).toLong())
    }

    fun tierForModel(model: String): ModelTier {
        val lower = model.lowercase()
        return when {
            lower.contains("lite") -> ModelTier.STANDARD
            else -> ModelTier.THINKING
        }
    }

    fun estimateCostDisplay(credits: Long): String {
        return when {
            credits >= 10_000 -> String.format("%.1fK", credits / 1_000.0)
            else -> credits.toString()
        }
    }
}
