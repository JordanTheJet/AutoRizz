package com.autorizz.backend.cloudflare

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
    fun provideAuthService(impl: CloudflareAuthService): AuthService = impl

    @Provides
    @Singleton
    fun provideCreditService(impl: CloudflareCreditService): CreditService = impl

    @Provides
    @Singleton
    fun provideProxyService(impl: CloudflareProxyService): ProxyService = impl

    @Provides
    @Singleton
    fun provideSyncService(impl: CloudflareSyncService): SyncService = impl

    @Provides
    @Singleton
    fun provideProfileService(impl: CloudflareProfileService): ProfileService = impl
}
