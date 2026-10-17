package com.finpilot.app.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.finpilot.app.module
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.time.Instant
import java.util.Date
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Exercises the authenticated route end to end using the local HS256 path (the Supabase CLI demo secret).
 * The asymmetric ES256 path is covered by [SupabaseJwtVerifierFactoryTest].
 */
class MeRouteTest {

    private val secret = "super-secret-jwt-token-with-at-least-32-characters-long"
    private val supabaseUrl = "http://127.0.0.1:54321"
    private val userId = UUID.randomUUID().toString()

    private fun ApplicationTestBuilder.configureApp(env: String = "local", hs256Secret: String? = secret) {
        environment {
            config = MapApplicationConfig(
                "finpilot.env" to env,
                "finpilot.auth.supabaseUrl" to supabaseUrl,
                "finpilot.auth.hs256Secret" to (hs256Secret ?: ""),
            )
        }
        application { module() }
    }

    private fun token(expiresAt: Instant = Instant.now().plusSeconds(3600), subject: String = userId): String =
        JWT.create()
            .withIssuer("$supabaseUrl/auth/v1")
            .withSubject(subject)
            .withAudience("authenticated")
            .withClaim("role", "authenticated")
            .withClaim("email", "chris@example.test")
            .withExpiresAt(Date.from(expiresAt))
            .sign(Algorithm.HMAC256(secret))

    @Test
    fun returnsTheCallerIdentityForAValidToken() = testApplication {
        configureApp()

        val response = client.get("/api/v1/me") { header(HttpHeaders.Authorization, "Bearer ${token()}") }

        assertEquals(HttpStatusCode.OK, response.status)
        val data = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("data").jsonObject
        assertEquals(userId, data.getValue("user_id").jsonPrimitive.content)
        assertEquals("chris@example.test", data.getValue("email").jsonPrimitive.content)
    }

    @Test
    fun rejectsRequestsWithoutABearerToken() = testApplication {
        configureApp()

        val response = client.get("/api/v1/me")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val error = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("error").jsonObject
        assertEquals("UNAUTHORIZED", error.getValue("code").jsonPrimitive.content)
    }

    @Test
    fun rejectsExpiredTokens() = testApplication {
        configureApp()

        val response = client.get("/api/v1/me") {
            header(HttpHeaders.Authorization, "Bearer ${token(expiresAt = Instant.now().minusSeconds(600))}")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun rejectsTokensWhoseSubjectIsNotAUuid() = testApplication {
        configureApp()

        val response = client.get("/api/v1/me") {
            header(HttpHeaders.Authorization, "Bearer ${token(subject = "service-account")}")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun rejectsHs256TokensWhenNoLocalSecretIsConfigured() = testApplication {
        configureApp(hs256Secret = null)

        val response = client.get("/api/v1/me") { header(HttpHeaders.Authorization, "Bearer ${token()}") }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun refusesToStartWithAnHs256SecretOutsideLocalEnvironment() {
        val failure = runCatching {
            testApplication {
                configureApp(env = "prod")
                client.get("/health")
            }
        }.exceptionOrNull()

        val messages = generateSequence(failure) { it.cause }.mapNotNull { it.message }.joinToString(" | ")
        assertTrue(
            messages.contains("AUTH_HS256_SECRET"),
            "startup must fail because of the HS256 guard, but got: $messages",
        )
    }
}
