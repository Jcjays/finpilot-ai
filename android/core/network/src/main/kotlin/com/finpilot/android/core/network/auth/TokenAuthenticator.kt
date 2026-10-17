package com.finpilot.android.core.network.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * On a 401 from the FinPilot API, refreshes the Supabase session once and retries with the new token.
 * A second 401 for the same call (visible through `priorResponse`) gives up so a dead session cannot loop.
 */
class TokenAuthenticator(
    apiBaseUrl: String,
    private val refresh: suspend () -> String?,
) : Authenticator {

    private val apiHost = ApiHostMatcher(apiBaseUrl)

    override fun authenticate(route: Route?, response: Response): Request? {
        if (!apiHost.matches(response.request.url) || response.priorResponse != null) return null
        val token = runBlocking { refresh() } ?: return null
        return response.request.newBuilder().header("Authorization", "Bearer $token").build()
    }
}
