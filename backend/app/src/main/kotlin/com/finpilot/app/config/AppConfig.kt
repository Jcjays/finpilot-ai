package com.finpilot.app.config

import io.ktor.server.config.ApplicationConfig

enum class AppEnvironment { LOCAL, CI, PROD }

enum class AiProvider { MOCK, CLAUDE }

data class AuthConfig(
    val supabaseUrl: String,
    val audience: String,
    /** Legacy/local HS256 secret. Only permitted in [AppEnvironment.LOCAL]; null everywhere else. */
    val hs256Secret: String?,
    val leewaySeconds: Long,
) {
    val issuer: String get() = "$supabaseUrl/auth/v1"
    val jwksUrl: String get() = "$supabaseUrl/auth/v1/.well-known/jwks.json"
}

data class AppConfig(
    val environment: AppEnvironment,
    val aiProvider: AiProvider,
    val auth: AuthConfig,
) {
    init {
        check(environment == AppEnvironment.LOCAL || auth.hs256Secret == null) {
            "AUTH_HS256_SECRET is only allowed when APP_ENV=local (current env: ${environment.name.lowercase()}). " +
                "Production verification must use the Supabase JWKS endpoint."
        }
    }

    companion object {
        /** The Supabase CLI local stack (`npx supabase start`). */
        const val LOCAL_SUPABASE_URL = "http://127.0.0.1:54321"

        fun from(config: ApplicationConfig): AppConfig = AppConfig(
            environment = config.enumOrDefault("finpilot.env", AppEnvironment.LOCAL),
            aiProvider = config.enumOrDefault("finpilot.ai.provider", AiProvider.MOCK),
            auth = AuthConfig(
                supabaseUrl = (config.propertyOrNull("finpilot.auth.supabaseUrl")?.getString() ?: LOCAL_SUPABASE_URL)
                    .trimEnd('/'),
                audience = config.propertyOrNull("finpilot.auth.audience")?.getString() ?: "authenticated",
                hs256Secret = config.propertyOrNull("finpilot.auth.hs256Secret")?.getString()?.trim()?.ifEmpty { null },
                leewaySeconds = config.propertyOrNull("finpilot.auth.leewaySeconds")?.getString()?.toLong() ?: 30L,
            ),
        )

        private inline fun <reified E : Enum<E>> ApplicationConfig.enumOrDefault(path: String, default: E): E {
            val raw = propertyOrNull(path)?.getString()?.trim().orEmpty()
            if (raw.isEmpty()) return default
            val allowed = enumValues<E>()
            return allowed.firstOrNull { it.name.equals(raw, ignoreCase = true) }
                ?: throw IllegalArgumentException(
                    "Invalid value '$raw' for $path; expected one of " +
                        allowed.joinToString { it.name.lowercase() },
                )
        }
    }
}
