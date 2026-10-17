package com.finpilot.app.plugins

import io.ktor.http.ContentType
import io.ktor.openapi.OpenApiDoc
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.openapi.plus
import io.ktor.server.routing.routing
import io.ktor.server.routing.routingRoot

private val apiInfo = OpenApiInfo(
    title = "FinPilot AI API",
    version = "0.1.0",
    description = "Personal finance API: transactions, budgets, savings goals, " +
        "receipt extraction and the Claude assistant.",
)

/**
 * Serves the OpenAPI document generated from the routing tree (plus compile-time metadata from the
 * Ktor OpenAPI compiler extension) at /openapi.json and an interactive Swagger UI at /swagger.
 */
fun Application.configureDocumentation() {
    routing {
        get("/openapi.json") {
            val doc = OpenApiDoc(info = apiInfo) + call.application.routingRoot.descendants()
            call.respond(doc)
        }
        swaggerUI(path = "swagger") {
            info = apiInfo
            source = OpenApiDocSource.Routing(contentType = ContentType.Application.Json)
        }
    }
}
