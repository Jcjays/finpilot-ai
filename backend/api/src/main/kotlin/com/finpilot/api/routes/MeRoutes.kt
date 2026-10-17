package com.finpilot.api.routes

import com.finpilot.api.ApiResponse
import com.finpilot.api.ErrorCodes
import com.finpilot.api.auth.UserPrincipal
import com.finpilot.api.respondError
import com.finpilot.domain.service.ProfileService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/**
 * Returns the caller's profile, creating it on first sign-in. Must be mounted inside an `authenticate` block.
 *
 * Response: 200 application/json [ApiResponse<MeResponse>] The caller's profile
 * Response: 401 application/json [com.finpilot.api.ApiErrorResponse] Missing or invalid access token
 * Response: 503 application/json [com.finpilot.api.ApiErrorResponse] No database is configured
 */
fun Route.meRoutes(profileService: ProfileService?) {
    get("/me") {
        val principal = call.principal<UserPrincipal>()
            ?: return@get call.respondError(HttpStatusCode.Unauthorized, ErrorCodes.UNAUTHORIZED, "Not authenticated")
        if (profileService == null) {
            return@get call.respondError(
                HttpStatusCode.ServiceUnavailable,
                ErrorCodes.DATABASE_UNAVAILABLE,
                "The database is not configured",
            )
        }
        val profile = profileService.ensureProfile(principal.userId, principal.email)
        call.respond(ApiResponse(MeResponse.from(profile)))
    }
}
