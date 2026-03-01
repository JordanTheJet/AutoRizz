package com.autorizz.dating.convo

import com.autorizz.dating.DatingConfig
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Manages reply timing to appear natural.
 * Enforces configurable delays and active hours.
 */
@Singleton
class TimingEngine @Inject constructor(
    private val datingConfig: DatingConfig
) {
    /**
     * Get a random delay between min and max reply delay (in minutes).
     */
    fun getRandomDelay(): Int {
        val min = datingConfig.minReplyDelay
        val max = datingConfig.maxReplyDelay
        return Random.nextInt(min, max + 1)
    }

    /**
     * Check if the current time is within the user's active hours.
     */
    fun isWithinActiveHours(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val start = datingConfig.activeHoursStart
        val end = datingConfig.activeHoursEnd

        return if (start <= end) {
            hour in start until end
        } else {
            // Handles overnight ranges like 22-6
            hour >= start || hour < end
        }
    }

    /**
     * Calculate milliseconds until the next active window starts.
     * Returns 0 if currently in active hours.
     */
    fun msUntilActiveHours(): Long {
        if (isWithinActiveHours()) return 0

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, datingConfig.activeHoursStart)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    /**
     * Check if a Bumble match is about to expire (within 4 hours of 24hr deadline).
     */
    fun isBumbleMatchUrgent(matchedAtMs: Long): Boolean {
        val hoursRemaining = (matchedAtMs + 24 * 60 * 60 * 1000 - System.currentTimeMillis()) / (1000 * 60 * 60)
        return hoursRemaining in 0..4
    }
}
