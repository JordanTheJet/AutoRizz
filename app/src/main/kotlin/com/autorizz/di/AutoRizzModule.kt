package com.autorizz.di

import com.autorizz.mode.AutoRizzProviderManager
import com.cellclaw.provider.ProviderManagerContract
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AutoRizzModule {

    @Provides
    @Singleton
    fun provideProviderManagerContract(
        cellBreakProviderManager: AutoRizzProviderManager
    ): ProviderManagerContract = cellBreakProviderManager
}
