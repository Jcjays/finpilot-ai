package com.finpilot.app

import com.finpilot.api.routes.HealthStatus
import com.finpilot.api.routes.healthRoutes
import com.finpilot.api.routes.meRoutes
import com.finpilot.app.auth.SupabaseJwtVerifierFactory
import com.finpilot.app.config.AppConfig
import com.finpilot.app.plugins.SUPABASE_AUTH
import com.finpilot.app.plugins.configureAuthentication
import com.finpilot.app.plugins.configureDependencyInjection
import com.finpilot.app.plugins.configureDocumentation
import com.finpilot.app.plugins.configureErrorHandling
import com.finpilot.app.plugins.configureMonitoring
import com.finpilot.app.plugins.configureSerialization
import com.finpilot.data.db.DatabaseHealth
import com.finpilot.domain.service.ProfileService
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.get
import org.koin.ktor.ext.getKoin

const val API_V1 = "/api/v1"

/** Application module referenced from application.conf; EngineMain is the entry point. */
fun Application.module() {
    configureDependencyInjection()
    configureMonitoring()
    configureSerialization()
    configureErrorHandling()
    configureAuthentication(verifierFactory = get<SupabaseJwtVerifierFactory>())
    configureDocumentation()

    val appConfig = get<AppConfig>()
    val databaseHealth = getKoin().getOrNull<DatabaseHealth>()
    val profileService = getKoin().getOrNull<ProfileService>()

    routing {
        healthRoutes {
            HealthStatus(
                status = "ok",
                db = databaseHealth?.status() ?: "not_configured",
                aiProvider = appConfig.aiProvider.name.lowercase(),
            )
        }
        route(API_V1) {
            authenticate(SUPABASE_AUTH) {
                meRoutes(profileService)
            }
        }
    }
}
