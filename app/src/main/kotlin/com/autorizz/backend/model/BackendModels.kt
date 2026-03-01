package com.autorizz.backend.model

data class UserProfileData(
    val id: String,
    val email: String,
    val displayName: String?,
    val creditBalance: Long,
    val referralCode: String?,
    val createdAt: Long
)

data class ProfileUpdates(
    val displayName: String? = null,
    val autoRefillEnabled: Boolean? = null,
    val autoRefillPack: String? = null,
    val autoRefillThreshold: Long? = null
)
