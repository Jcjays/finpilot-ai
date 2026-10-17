package com.finpilot.app.plugins

import com.finpilot.api.ErrorCodes
import com.finpilot.api.auth.UserPrincipal
import com.finpilot.api.respondError
import com.finpilot.app.auth.SupabaseJwtVerifierFactory
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.AuthScheme
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import java.util.UUID

const val SUPABASE_AUTH = "supabase"

fun Application.configureAuthentication(verifierFactory: SupabaseJwtVerifierFactory) {
    install(Authentication) {
        jwt(SUPABASE_AUTH) {
            realm = "FinPilot AI"
            verifier { header -> header.bearerToken()?.let(verifierFactory::verifierFor) }
            validate { credential ->
                val userId = credential.payload.subject
                    ?.let { subject -> runCatching { UUID.fromString(subject) }.getOrNull() }
                userId?.let { UserPrincipal(userId = it, email = credential.payload.getClaim("email").asString()) }
            }
            challenge { _, _ ->
                call.respondError(
                    HttpStatusCode.Unauthorized,
                    ErrorCodes.UNAUTHORIZED,
                    "Missing or invalid access token",
                )
            }
        }
    }
}

private fun HttpAuthHeader.bearerToken(): String? =
    (this as? HttpAuthHeader.Single)?.takeIf { it.authScheme.equals(AuthScheme.Bearer, ignoreCase = true) }?.blob
