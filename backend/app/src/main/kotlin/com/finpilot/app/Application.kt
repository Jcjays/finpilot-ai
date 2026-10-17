package com.finpilot.app

import com.finpilot.api.routes.HealthStatus
import com.finpilot.api.routes.healthRoutes
import com.finpilot.api.routes.meRoutes
import com.finpilot.app.config.AppConfig
import com.finpilot.app.plugins.SUPABASE_AUTH
import com.finpilot.app.plugins.configureAuthentication
import com.finpilot.app.plugins.configureDependencyInjection
import com.finpilot.app.plugins.configureDocumentation
import com.finpilot.app.plugins.configureErrorHandling
import com.finpilot.app.plugins.configureMonitoring
import com.finpilot.app.plugins.configureSerialization
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

const val API_V1 = "/api/v1"

/** Application module referenced from application.conf; EngineMain is the entry point. */
fun Application.module() {
    configureDependencyInjection()
    configureMonitoring()
    configureSerialization()
    configureErrorHandling()
    configureAuthentication(verifierFactory = inject<com.finpilot.app.auth.SupabaseJwtVerifierFactory>().value)
    configureDocumentation()

    val appConfig by inject<AppConfig>()

    routing {
        healthRoutes {
            HealthStatus(
                status = "ok",
                db = "not_configured",
                aiProvider = appConfig.aiProvider.name.lowercase(),
            )
        }
        route(API_V1) {
            authenticate(SUPABASE_AUTH) {
                meRoutes()
            }
        }
    }
}
