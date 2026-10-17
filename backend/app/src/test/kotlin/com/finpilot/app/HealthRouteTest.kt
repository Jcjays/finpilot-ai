package com.finpilot.app

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class HealthRouteTest {

    @Test
    fun healthEndpointReportsOkInsideTheDataEnvelope() = testApplication {
        application { module() }
        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Application.Json, response.contentType()?.withoutParameters())
        val data = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("data").jsonObject
        assertEquals("ok", data.getValue("status").jsonPrimitive.content)
        assertEquals("mock", data.getValue("ai_provider").jsonPrimitive.content)
        assertEquals("not_configured", data.getValue("db").jsonPrimitive.content)
    }

    @Test
    fun unknownRouteReturnsTheErrorEnvelopeWithARequestId() = testApplication {
        application { module() }
        val response = client.get("/definitely-not-a-route")

        assertEquals(HttpStatusCode.NotFound, response.status)
        val error = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("error").jsonObject
        assertEquals("NOT_FOUND", error.getValue("code").jsonPrimitive.content)
        val requestId = error.getValue("request_id").jsonPrimitive.content
        assertTrue(requestId.isNotBlank())
        assertEquals(response.headers["X-Request-Id"], requestId)
    }

    @Test
    fun openApiDocumentIsServedAsJson() = testApplication {
        application { module() }
        val response = client.get("/openapi.json")

        assertEquals(HttpStatusCode.OK, response.status)
        val doc = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(doc.getValue("openapi").jsonPrimitive.content.startsWith("3."))
        assertTrue(doc.getValue("paths").jsonObject.containsKey("/health"))
    }
}
