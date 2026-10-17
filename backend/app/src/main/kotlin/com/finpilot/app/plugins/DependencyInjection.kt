package com.finpilot.app.plugins

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.finpilot.app.auth.SupabaseJwtVerifierFactory
import com.finpilot.app.config.AppConfig
import io.ktor.server.application.Application
import io.ktor.server.application.install
import java.net.URI
import java.util.concurrent.TimeUnit
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

private const val JWKS_CACHE_SIZE = 10L
private const val JWKS_CACHE_MINUTES = 10L
private const val JWKS_FETCHES_PER_MINUTE = 10L

fun Application.configureDependencyInjection() {
    val appConfig = AppConfig.from(environment.config)
    install(Koin) {
        slf4jLogger()
        modules(
            module {
                single { appConfig }
                single<JwkProvider> {
                    // Supabase edge-caches the JWKS for ~10 minutes; do not cache longer or key revocation lags.
                    JwkProviderBuilder(URI.create(appConfig.auth.jwksUrl).toURL())
                        .cached(JWKS_CACHE_SIZE, JWKS_CACHE_MINUTES, TimeUnit.MINUTES)
                        .rateLimited(JWKS_FETCHES_PER_MINUTE, 1, TimeUnit.MINUTES)
                        .build()
                }
                single {
                    SupabaseJwtVerifierFactory(
                        jwkProvider = get(),
                        issuer = appConfig.auth.issuer,
                        audience = appConfig.auth.audience,
                        hs256Secret = appConfig.auth.hs256Secret,
                        leewaySeconds = appConfig.auth.leewaySeconds,
                    )
                }
            },
        )
    }
}
