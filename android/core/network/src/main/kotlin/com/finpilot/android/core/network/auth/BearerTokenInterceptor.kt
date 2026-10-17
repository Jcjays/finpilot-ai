package com.finpilot.android.core.network.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches `Authorization: Bearer <token>` to requests bound for the FinPilot API only.
 * Requests that already carry an Authorization header are left untouched.
 */
class BearerTokenInterceptor(
    apiBaseUrl: String,
    private val tokenProvider: suspend () -> String?,
) : Interceptor {

    private val apiHost = ApiHostMatcher(apiBaseUrl)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!apiHost.matches(request.url) || request.header(AUTHORIZATION) != null) {
            return chain.proceed(request)
        }
        val token = runBlocking { tokenProvider() } ?: return chain.proceed(request)
        return chain.proceed(request.newBuilder().header(AUTHORIZATION, "Bearer $token").build())
    }

    private companion object {
        const val AUTHORIZATION = "Authorization"
    }
}
