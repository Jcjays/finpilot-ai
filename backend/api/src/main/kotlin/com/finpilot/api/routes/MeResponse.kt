package com.finpilot.api.routes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MeResponse(
    @SerialName("user_id") val userId: String,
    val email: String?,
)
