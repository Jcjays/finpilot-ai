package com.finpilot.api.routes

import com.finpilot.api.ApiResponse
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get


/**
 * Liveness endpoint. Unauthenticated by design so load balancers and CI smoke tests can call it.
 *
 * Response: 200 application/json [ApiResponse<HealthStatus>] Service status, database state, and active AI provider
 */
fun Route.healthRoutes(health: () -> HealthStatus) {
    get("/health") {
        call.respond(ApiResponse(health()))
    }
}
