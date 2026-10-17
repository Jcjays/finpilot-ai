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

/** JDBC coordinates for the least-privilege API role. On Supabase Free use the session pooler (port 5432). */
data class DatabaseConfig(
    val jdbcUrl: String,
    val user: String,
    val password: String,
    val poolSize: Int,
)

data class AppConfig(
    val environment: AppEnvironment,
    val aiProvider: AiProvider,
    val auth: AuthConfig,
    /** Null when no DATABASE_URL is set: allowed outside prod so the API can boot for smoke tests. */
    val database: DatabaseConfig?,
) {
    init {
        check(environment == AppEnvironment.LOCAL || auth.hs256Secret == null) {
            "AUTH_HS256_SECRET is only allowed when APP_ENV=local (current env: ${environment.name.lowercase()}). " +
                "Production verification must use the Supabase JWKS endpoint."
        }
        check(environment != AppEnvironment.PROD || database != null) {
            "DATABASE_URL, DATABASE_USER and DATABASE_PASSWORD are required when APP_ENV=prod."
        }
    }

    companion object {
        /** The Supabase CLI local stack (`npx supabase start`). */
        const val LOCAL_SUPABASE_URL = "http://127.0.0.1:54321"
        private const val DEFAULT_LEEWAY_SECONDS = 30L
        private const val DEFAULT_POOL_SIZE = 4

        fun from(config: ApplicationConfig): AppConfig = AppConfig(
            environment = config.enumOrDefault("finpilot.env", AppEnvironment.LOCAL),
            aiProvider = config.enumOrDefault("finpilot.ai.provider", AiProvider.MOCK),
            auth = AuthConfig(
                supabaseUrl = (config.stringOrNull("finpilot.auth.supabaseUrl") ?: LOCAL_SUPABASE_URL).trimEnd('/'),
                audience = config.stringOrNull("finpilot.auth.audience") ?: "authenticated",
                hs256Secret = config.stringOrNull("finpilot.auth.hs256Secret"),
                leewaySeconds = config.stringOrNull("finpilot.auth.leewaySeconds")?.toLong() ?: DEFAULT_LEEWAY_SECONDS,
            ),
            database = config.stringOrNull("finpilot.database.url")?.let { url ->
                DatabaseConfig(
                    jdbcUrl = url,
                    user = config.property("finpilot.database.user").getString(),
                    password = config.property("finpilot.database.password").getString(),
                    poolSize = config.stringOrNull("finpilot.database.poolSize")?.toInt() ?: DEFAULT_POOL_SIZE,
                )
            },
        )

        /** Reads a string property, treating missing and blank values alike. */
        private fun ApplicationConfig.stringOrNull(path: String): String? =
            propertyOrNull(path)?.getString()?.trim()?.ifEmpty { null }

        private inline fun <reified E : Enum<E>> ApplicationConfig.enumOrDefault(path: String, default: E): E {
            val raw = stringOrNull(path) ?: return default
            val allowed = enumValues<E>()
            return allowed.firstOrNull { it.name.equals(raw, ignoreCase = true) }
                ?: throw IllegalArgumentException(
                    "Invalid value '$raw' for $path; expected one of " +
                        allowed.joinToString { it.name.lowercase() },
                )
        }
    }
}
