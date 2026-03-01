package com.autorizz.dating.swipe

/**
 * Interface for per-app swipe automation strategies.
 * Each dating app has different UI patterns, limits, and behaviors.
 */
interface SwipeStrategy {
    /** App identifier (hinge, tinder, bumble) */
    val appName: String

    /** Android package name */
    val appPackage: String

    /** Free-tier daily like limit */
    val dailyLikeLimit: Int

    /**
     * Navigate from wherever the app currently is to the swipe deck/discover feed.
     * Uses screen.read + app.automate to find and tap correct navigation elements.
     */
    suspend fun navigateToSwipeDeck()

    /**
     * Read the currently visible profile card and parse it into structured data.
     * Uses screen.read for text, optionally vision.analyze for photos.
     */
    suspend fun readCurrentProfile(): ProfileData?

    /**
     * Execute a "like" action on the current profile.
     * @param comment Optional comment (Hinge-specific: comment on a prompt/photo)
     */
    suspend fun executeLike(comment: String? = null)

    /** Execute a "pass" / skip action on the current profile. */
    suspend fun executePass()

    /**
     * Check for and dismiss any paywall/premium upsell popups.
     * @return true if a popup was detected and dismissed
     */
    suspend fun handlePopups(): Boolean

    /**
     * Check if the daily free-tier limit has been reached.
     * Detects "out of likes" screens or tracks internal count.
     */
    suspend fun isAtLimit(): Boolean

    /** Navigate to the matches/inbox screen for match checking. */
    suspend fun navigateToMatches()

    /** Check if a "match" celebration/popup is currently showing. */
    suspend fun isMatchPopupShowing(): Boolean

    /** Dismiss a match popup and continue swiping. */
    suspend fun dismissMatchPopup()
}
