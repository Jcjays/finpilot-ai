package com.finpilot.app.config

import io.ktor.server.config.ApplicationConfig

enum class AppEnvironment { LOCAL, CI, PROD }

enum class AiProvider { MOCK, CLAUDE }

data class AppConfig(
    val environment: AppEnvironment,
    val aiProvider: AiProvider,
) {
    companion object {
        fun from(config: ApplicationConfig): AppConfig = AppConfig(
            environment = config.enumOrDefault("finpilot.env", AppEnvironment.LOCAL),
            aiProvider = config.enumOrDefault("finpilot.ai.provider", AiProvider.MOCK),
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
