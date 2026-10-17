package com.finpilot.android.core.network.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

@Serializable
data class ApiEnvelope<T>(val data: T)

@Serializable
data class MeDto(
    @SerialName("user_id") val userId: String,
    val email: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val currency: String,
    val timezone: String,
)

interface FinPilotApi {
    @GET("api/v1/me")
    suspend fun me(): ApiEnvelope<MeDto>
}
