package com.autorizz.backend.supabase

import android.util.Log
import com.autorizz.backend.ProfileService
import com.autorizz.backend.model.ProfileUpdates
import com.autorizz.backend.model.UserProfileData
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseProfileService @Inject constructor(
    private val supabase: SupabaseClientProvider
) : ProfileService {

    override suspend fun getProfile(userId: String): Result<UserProfileData> {
        return try {
            // TODO: Decode full profile from Supabase postgrest
            Result.success(
                UserProfileData(
                    id = userId,
                    email = "",
                    displayName = null,
                    creditBalance = 0,
                    referralCode = null,
                    createdAt = 0
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get profile: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(userId: String, updates: ProfileUpdates): Result<Unit> {
        return try {
            // TODO: Update profile via Supabase postgrest
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile: ${e.message}")
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "SupabaseProfileService"
    }
}
