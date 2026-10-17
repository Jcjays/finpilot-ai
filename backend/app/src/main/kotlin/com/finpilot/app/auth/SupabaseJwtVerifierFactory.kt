package com.finpilot.app.auth

import com.auth0.jwk.JwkException
import com.auth0.jwk.JwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTDecodeException
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey

/**
 * Chooses a verifier for a Supabase access token based on its header.
 *
 * - `ES256` / `RS256`: the public key is looked up by `kid` in the project's JWKS (cached by the provider).
 * - `HS256`: accepted only when a local development secret is configured (never in CI or production).
 * - Anything else (including `none`) is rejected by returning null.
 *
 * Every verifier also enforces issuer, audience, the `authenticated` role and expiry with a small leeway.
 */
class SupabaseJwtVerifierFactory(
    private val jwkProvider: JwkProvider,
    private val issuer: String,
    private val audience: String,
    private val hs256Secret: String?,
    private val leewaySeconds: Long,
) {

    fun verifierFor(token: String): JWTVerifier? {
        val decoded = try {
            JWT.decode(token)
        } catch (_: JWTDecodeException) {
            return null
        }
        val algorithm = when (decoded.algorithm) {
            "ES256", "RS256" -> asymmetricAlgorithm(decoded.keyId, decoded.algorithm)
            "HS256" -> hs256Secret?.let(Algorithm::HMAC256)
            else -> null
        } ?: return null

        return JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("role", REQUIRED_ROLE)
            .acceptLeeway(leewaySeconds)
            .build()
    }

    private fun asymmetricAlgorithm(keyId: String?, algorithmName: String): Algorithm? {
        if (keyId.isNullOrBlank()) return null
        val publicKey = try {
            jwkProvider.get(keyId).publicKey
        } catch (_: JwkException) {
            return null
        }
        return when (algorithmName) {
            "ES256" -> (publicKey as? ECPublicKey)?.let { Algorithm.ECDSA256(it, null) }
            "RS256" -> (publicKey as? RSAPublicKey)?.let { Algorithm.RSA256(it, null) }
            else -> null
        }
    }

    private companion object {
        const val REQUIRED_ROLE = "authenticated"
    }
}
