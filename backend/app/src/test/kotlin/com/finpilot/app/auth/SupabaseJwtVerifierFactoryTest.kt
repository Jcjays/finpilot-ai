package com.finpilot.app.auth

import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.SigningKeyNotFoundException
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SupabaseJwtVerifierFactoryTest {

    private val issuer = "https://project.supabase.test/auth/v1"
    private val kid = "key-2026-10"
    private val keyPair = KeyPairGenerator.getInstance("EC")
        .apply { initialize(ECGenParameterSpec("secp256r1")) }
        .generateKeyPair()
    private val publicKey = keyPair.public as ECPublicKey
    private val privateKey = keyPair.private as ECPrivateKey
    private val userId = UUID.randomUUID().toString()

    private val knownKeys: JwkProvider = JwkProvider { requestedKid ->
        if (requestedKid == kid) ecJwk(kid, publicKey) else throw SigningKeyNotFoundException("unknown kid", null)
    }

    private fun factory(hs256Secret: String? = null) = SupabaseJwtVerifierFactory(
        jwkProvider = knownKeys,
        issuer = issuer,
        audience = "authenticated",
        hs256Secret = hs256Secret,
        leewaySeconds = 30,
    )

    private fun es256Token(
        subject: String = userId,
        iss: String = issuer,
        aud: String? = "authenticated",
        expiresAt: Instant = Instant.now().plusSeconds(3600),
        keyId: String = kid,
    ): String {
        val builder = JWT.create()
            .withKeyId(keyId)
            .withIssuer(iss)
            .withSubject(subject)
            .withClaim("role", "authenticated")
            .withClaim("email", "chris@example.test")
            .withExpiresAt(Date.from(expiresAt))
        if (aud != null) builder.withAudience(aud)
        return builder.sign(Algorithm.ECDSA256(publicKey, privateKey))
    }

    @Test
    fun acceptsEs256TokenSignedByAKnownKey() {
        val token = es256Token()

        val verifier = assertNotNull(factory().verifierFor(token))
        val decoded = verifier.verify(token)

        assertEquals(userId, decoded.subject)
        assertEquals("chris@example.test", decoded.getClaim("email").asString())
    }

    @Test
    fun rejectsTokenSignedByAnUnknownKey() {
        val token = es256Token(keyId = "rotated-away")

        assertNull(factory().verifierFor(token))
    }

    @Test
    fun rejectsTokenFromAnotherIssuer() {
        val token = es256Token(iss = "https://evil.example/auth/v1")

        val verifier = assertNotNull(factory().verifierFor(token))
        assertFailsWith<JWTVerificationException> { verifier.verify(token) }
    }

    @Test
    fun rejectsTokenWithoutTheAuthenticatedAudience() {
        val token = es256Token(aud = null)

        val verifier = assertNotNull(factory().verifierFor(token))
        assertFailsWith<JWTVerificationException> { verifier.verify(token) }
    }

    @Test
    fun rejectsTokenExpiredBeyondLeeway() {
        val token = es256Token(expiresAt = Instant.now().minusSeconds(120))

        val verifier = assertNotNull(factory().verifierFor(token))
        assertFailsWith<JWTVerificationException> { verifier.verify(token) }
    }

    @Test
    fun acceptsTokenExpiredWithinLeeway() {
        val token = es256Token(expiresAt = Instant.now().minusSeconds(10))

        val verifier = assertNotNull(factory().verifierFor(token))
        assertEquals(userId, verifier.verify(token).subject)
    }

    @Test
    fun rejectsHs256TokenWhenNoLocalSecretIsConfigured() {
        val token = hs256Token("super-secret-jwt-token-with-at-least-32-characters-long")

        assertNull(factory(hs256Secret = null).verifierFor(token))
    }

    @Test
    fun acceptsHs256TokenWhenTheLocalSecretMatches() {
        val secret = "super-secret-jwt-token-with-at-least-32-characters-long"
        val token = hs256Token(secret)

        val verifier = assertNotNull(factory(hs256Secret = secret).verifierFor(token))
        assertEquals(userId, verifier.verify(token).subject)
    }

    @Test
    fun rejectsHs256TokenSignedWithADifferentSecret() {
        val configuredSecret = "super-secret-jwt-token-with-at-least-32-characters-long"
        val token = hs256Token("not-the-configured-secret-but-still-long-enough-for-hmac")

        val verifier = assertNotNull(factory(hs256Secret = configuredSecret).verifierFor(token))
        assertFailsWith<JWTVerificationException> { verifier.verify(token) }
    }

    @Test
    fun rejectsUnsignedNoneAlgorithmTokens() {
        val header = base64Url("""{"alg":"none","typ":"JWT"}""".toByteArray())
        val payload = base64Url("""{"sub":"$userId","iss":"$issuer","aud":"authenticated"}""".toByteArray())
        val token = "$header.$payload."

        assertNull(factory().verifierFor(token))
    }

    @Test
    fun rejectsGarbageThatIsNotAJwt() {
        assertNull(factory().verifierFor("definitely.not-a.jwt-token"))
    }

    private fun hs256Token(secret: String): String = JWT.create()
        .withIssuer(issuer)
        .withSubject(userId)
        .withAudience("authenticated")
        .withClaim("role", "authenticated")
        .withExpiresAt(Date.from(Instant.now().plusSeconds(3600)))
        .sign(Algorithm.HMAC256(secret))

    private fun ecJwk(keyId: String, key: ECPublicKey): Jwk = Jwk.fromValues(
        mapOf(
            "kid" to keyId,
            "kty" to "EC",
            "alg" to "ES256",
            "use" to "sig",
            "crv" to "P-256",
            "x" to base64Url(key.w.affineX.toUnsignedBytes(32)),
            "y" to base64Url(key.w.affineY.toUnsignedBytes(32)),
        ),
    )

    private fun base64Url(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun java.math.BigInteger.toUnsignedBytes(size: Int): ByteArray {
        val raw = toByteArray()
        val stripped = if (raw.size > size && raw[0] == 0.toByte()) raw.copyOfRange(1, raw.size) else raw
        return ByteArray(size - stripped.size) + stripped
    }
}
