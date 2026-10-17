package com.finpilot.api.routes

import com.finpilot.api.ApiResponse
import com.finpilot.api.ErrorCodes
import com.finpilot.api.auth.UserPrincipal
import com.finpilot.api.respondError
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/**
 * Returns the identity of the authenticated caller. Must be mounted inside an `authenticate` block.
 *
 * Response: 200 application/json [ApiResponse<MeResponse>] The caller's user id and email
 * Response: 401 application/json [com.finpilot.api.ApiErrorResponse] Missing or invalid access token
 */
fun Route.meRoutes() {
    get("/me") {
        val principal = call.principal<UserPrincipal>()
            ?: return@get call.respondError(HttpStatusCode.Unauthorized, ErrorCodes.UNAUTHORIZED, "Not authenticated")
        call.respond(ApiResponse(MeResponse(userId = principal.userId.toString(), email = principal.email)))
    }
}
