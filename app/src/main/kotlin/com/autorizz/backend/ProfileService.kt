package com.autorizz.backend

import com.autorizz.backend.model.UserProfileData
import com.autorizz.backend.model.ProfileUpdates

/**
 * Backend-agnostic user profile operations.
 */
interface ProfileService {
    suspend fun getProfile(userId: String): Result<UserProfileData>
    suspend fun updateProfile(userId: String, updates: ProfileUpdates): Result<Unit>
}
