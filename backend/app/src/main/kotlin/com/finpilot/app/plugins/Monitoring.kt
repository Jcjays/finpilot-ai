package com.finpilot.app.plugins

import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.request.path
import java.util.UUID
import org.slf4j.event.Level

private const val MAX_REQUEST_ID_LENGTH = 64

/** Request ids are echoed back to clients, so only accept a conservative character set. */
private fun isAcceptableRequestId(id: String): Boolean =
    id.isNotBlank() && id.length <= MAX_REQUEST_ID_LENGTH && id.all { it.isLetterOrDigit() || it == '-' }

fun Application.configureMonitoring() {
    install(DefaultHeaders) {
        header("X-Content-Type-Options", "nosniff")
    }
    install(CallId) {
        retrieveFromHeader(HttpHeaders.XRequestId)
        generate { UUID.randomUUID().toString() }
        verify(::isAcceptableRequestId)
        replyToHeader(HttpHeaders.XRequestId)
    }
    install(CallLogging) {
        level = Level.INFO
        callIdMdc("request_id")
        filter { call -> !call.request.path().startsWith("/health") }
    }
}
