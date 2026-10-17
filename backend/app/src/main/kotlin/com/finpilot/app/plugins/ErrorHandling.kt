package com.finpilot.app.plugins

import com.finpilot.api.ErrorCodes
import com.finpilot.api.respondError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.statuspages.StatusPages

fun Application.configureErrorHandling() {
    install(StatusPages) {
        status(HttpStatusCode.NotFound) { call, status ->
            call.respondError(status, ErrorCodes.NOT_FOUND, "Resource not found")
        }
        status(HttpStatusCode.MethodNotAllowed) { call, status ->
            call.respondError(status, ErrorCodes.METHOD_NOT_ALLOWED, "Method not allowed")
        }
        exception<NotFoundException> { call, _ ->
            call.respondError(HttpStatusCode.NotFound, ErrorCodes.NOT_FOUND, "Resource not found")
        }
        exception<BadRequestException> { call, cause ->
            call.respondError(
                HttpStatusCode.BadRequest,
                ErrorCodes.VALIDATION_ERROR,
                cause.message ?: "Malformed request",
            )
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled error for request_id=${call.callId}", cause)
            call.respondError(HttpStatusCode.InternalServerError, ErrorCodes.INTERNAL, "Unexpected server error")
        }
    }
}
