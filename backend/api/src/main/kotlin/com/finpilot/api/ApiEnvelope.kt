package com.finpilot.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.callid.callId
import io.ktor.server.response.respond
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Success envelope: every 2xx JSON body is `{"data": ..., "meta": {...}}`. */
@Serializable
data class ApiResponse<T>(
    val data: T,
    val meta: ResponseMeta? = null,
)

@Serializable
data class ResponseMeta(
    @SerialName("next_cursor") val nextCursor: String? = null,
)

/** Error envelope: every non-2xx JSON body is `{"error": {...}}`. Never leaks stack traces or SQL. */
@Serializable
data class ApiErrorResponse(val error: ApiError)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val details: List<ErrorDetail> = emptyList(),
    @SerialName("request_id") val requestId: String? = null,
)

@Serializable
data class ErrorDetail(
    val field: String? = null,
    val issue: String,
)

object ErrorCodes {
    const val VALIDATION_ERROR = "VALIDATION_ERROR"
    const val UNAUTHORIZED = "UNAUTHORIZED"
    const val NOT_FOUND = "NOT_FOUND"
    const val METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED"
    const val CONFLICT = "CONFLICT"
    const val RATE_LIMITED = "RATE_LIMITED"
    const val INTERNAL = "INTERNAL"
}

suspend fun ApplicationCall.respondError(
    status: HttpStatusCode,
    code: String,
    message: String,
    details: List<ErrorDetail> = emptyList(),
) {
    respond(
        status,
        ApiErrorResponse(
            ApiError(code = code, message = message, details = details, requestId = callId),
        ),
    )
}
