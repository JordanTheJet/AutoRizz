package com.autorizz.backend.supabase

import com.autorizz.backend.AuthService
import com.autorizz.backend.CreditService
import com.autorizz.backend.ProfileService
import com.autorizz.backend.ProxyService
import com.autorizz.backend.SyncService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackendModule {

    @Provides
    @Singleton
    fun provideAuthService(impl: SupabaseAuthService): AuthService = impl

    @Provides
    @Singleton
    fun provideCreditService(impl: SupabaseCreditService): CreditService = impl

    @Provides
    @Singleton
    fun provideProxyService(impl: SupabaseProxyService): ProxyService = impl

    @Provides
    @Singleton
    fun provideSyncService(impl: SupabaseSyncService): SyncService = impl

    @Provides
    @Singleton
    fun provideProfileService(impl: SupabaseProfileService): ProfileService = impl
}
