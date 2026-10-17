package com.finpilot.app.plugins

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.finpilot.app.auth.SupabaseJwtVerifierFactory
import com.finpilot.app.config.AppConfig
import com.finpilot.app.config.DatabaseConfig
import com.finpilot.data.db.DatabaseFactory
import com.finpilot.data.db.DatabaseHealth
import com.finpilot.data.db.DatabaseSettings
import com.finpilot.data.db.ExposedTransactor
import com.finpilot.data.repository.ExposedProfileRepository
import com.finpilot.domain.repository.ProfileRepository
import com.finpilot.domain.repository.Transactor
import com.finpilot.domain.service.ProfileService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.module.Module
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
        modules(listOfNotNull(coreModule(appConfig), appConfig.database?.let(::databaseModule)))
    }
}

private fun coreModule(appConfig: AppConfig): Module = module {
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
}

private fun databaseModule(database: DatabaseConfig): Module = module {
    single<DataSource> {
        DatabaseFactory.dataSource(
            DatabaseSettings(
                jdbcUrl = database.jdbcUrl,
                user = database.user,
                password = database.password,
                poolSize = database.poolSize,
            ),
        )
    }
    single<Database> { DatabaseFactory.connect(get()) }
    single { DatabaseHealth(get()) }
    single<Transactor> { ExposedTransactor(get()) }
    single<ProfileRepository> { ExposedProfileRepository() }
    single { ProfileService(get(), get()) }
}
