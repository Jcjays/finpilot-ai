package com.finpilot.android.core.network.auth

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BearerTokenInterceptorTest {

    private val apiBaseUrl = "https://api.finpilot.test/"

    /** Runs [request] through a real OkHttp chain; the last interceptor records it instead of using the network. */
    private fun send(interceptor: BearerTokenInterceptor, request: Request): Request {
        var forwarded: Request? = null
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor(
                Interceptor { chain ->
                    forwarded = chain.request()
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("{}".toResponseBody("application/json".toMediaType()))
                        .build()
                },
            )
            .build()
        client.newCall(request).execute().close()
        return checkNotNull(forwarded)
    }

    @Test
    fun attachesTheAccessTokenToRequestsForTheFinPilotApi() {
        val interceptor = BearerTokenInterceptor(apiBaseUrl) { "access-123" }

        val forwarded = send(interceptor, Request.Builder().url("https://api.finpilot.test/api/v1/me").build())

        assertEquals("Bearer access-123", forwarded.header("Authorization"))
    }

    @Test
    fun neverLeaksTheTokenToOtherHosts() {
        val interceptor = BearerTokenInterceptor(apiBaseUrl) { "access-123" }

        val forwarded = send(interceptor, Request.Builder().url("https://project.supabase.co/auth/v1/token").build())

        assertNull(forwarded.header("Authorization"))
    }

    @Test
    fun sendsTheRequestUnauthenticatedWhenThereIsNoSession() {
        val interceptor = BearerTokenInterceptor(apiBaseUrl) { null }

        val forwarded = send(interceptor, Request.Builder().url("https://api.finpilot.test/api/v1/me").build())

        assertNull(forwarded.header("Authorization"))
    }

    @Test
    fun doesNotOverrideAnExplicitAuthorizationHeader() {
        val interceptor = BearerTokenInterceptor(apiBaseUrl) { "access-123" }
        val request = Request.Builder()
            .url("https://api.finpilot.test/api/v1/me")
            .header("Authorization", "Basic abc")
            .build()

        val forwarded = send(interceptor, request)

        assertEquals("Basic abc", forwarded.header("Authorization"))
    }
}
