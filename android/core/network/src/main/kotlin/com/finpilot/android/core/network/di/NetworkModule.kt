package com.finpilot.android.core.network.di

import com.finpilot.android.core.network.NetworkConfig
import com.finpilot.android.core.network.api.FinPilotApi
import com.finpilot.android.core.network.auth.AuthRepository
import com.finpilot.android.core.network.auth.BearerTokenInterceptor
import com.finpilot.android.core.network.auth.SupabaseAuthRepository
import com.finpilot.android.core.network.auth.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TIMEOUT_SECONDS = 30L

    @Provides
    @Singleton
    fun supabaseClient(config: NetworkConfig): SupabaseClient =
        createSupabaseClient(supabaseUrl = config.supabaseUrl, supabaseKey = config.supabasePublishableKey) {
            install(Auth)
            httpEngine = OkHttp.create()
        }

    @Provides
    @Singleton
    fun authRepository(client: SupabaseClient): AuthRepository =
        SupabaseAuthRepository(client, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    @Provides
    @Singleton
    fun okHttpClient(config: NetworkConfig, authRepository: AuthRepository): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(BearerTokenInterceptor(config.apiBaseUrl) { authRepository.accessToken() })
        .authenticator(TokenAuthenticator(config.apiBaseUrl) { authRepository.refreshSession() })
        // BASIC logs method, URL and status only; never headers or bodies, so tokens stay out of logcat.
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
        .build()

    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun retrofit(config: NetworkConfig, client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(config.apiBaseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun finPilotApi(retrofit: Retrofit): FinPilotApi = retrofit.create()
}
