package com.finpilot.api.routes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthStatus(
    val status: String,
    val db: String,
    @SerialName("ai_provider") val aiProvider: String,
)
