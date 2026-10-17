package com.finpilot.android.core.network.auth

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TokenAuthenticatorTest {

    private val apiBaseUrl = "https://api.finpilot.test/"

    private fun apiRequest(token: String = "stale") = Request.Builder()
        .url("https://api.finpilot.test/api/v1/me")
        .header("Authorization", "Bearer $token")
        .build()

    private fun unauthorized(request: Request, prior: Response? = null): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(401)
        .message("Unauthorized")
        .apply { prior?.let { priorResponse(it) } }
        .build()

    @Test
    fun retriesOnceWithAFreshlyRefreshedToken() {
        var refreshes = 0
        val authenticator = TokenAuthenticator(apiBaseUrl) { refreshes++; "fresh" }

        val retry = authenticator.authenticate(null, unauthorized(apiRequest()))

        assertEquals("Bearer fresh", retry?.header("Authorization"))
        assertEquals(1, refreshes)
    }

    @Test
    fun givesUpWhenTheSessionCannotBeRefreshed() {
        val authenticator = TokenAuthenticator(apiBaseUrl) { null }

        assertNull(authenticator.authenticate(null, unauthorized(apiRequest())))
    }

    @Test
    fun doesNotLoopWhenTheRetriedRequestIsRejectedAgain() {
        val authenticator = TokenAuthenticator(apiBaseUrl) { "fresh-again" }
        val first = unauthorized(apiRequest("stale"))
        val second = unauthorized(apiRequest("fresh"), prior = first)

        assertNull(authenticator.authenticate(null, second))
    }

    @Test
    fun ignoresResponsesFromOtherHosts() {
        var refreshes = 0
        val authenticator = TokenAuthenticator(apiBaseUrl) { refreshes++; "fresh" }
        val supabase = Request.Builder().url("https://project.supabase.co/auth/v1/user").build()

        assertNull(authenticator.authenticate(null, unauthorized(supabase)))
        assertEquals(0, refreshes)
    }
}
