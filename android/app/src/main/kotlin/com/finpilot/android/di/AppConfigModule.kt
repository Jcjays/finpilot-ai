package com.finpilot.android.di

import com.finpilot.android.BuildConfig
import com.finpilot.android.core.network.NetworkConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {

    @Provides
    @Singleton
    fun networkConfig(): NetworkConfig = NetworkConfig(
        apiBaseUrl = BuildConfig.API_BASE_URL,
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabasePublishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
    )
}
