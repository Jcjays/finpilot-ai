package com.finpilot.api.routes

import com.finpilot.domain.model.Profile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MeResponse(
    @SerialName("user_id") val userId: String,
    val email: String?,
    @SerialName("display_name") val displayName: String?,
    val currency: String,
    val timezone: String,
) {
    companion object {
        fun from(profile: Profile) = MeResponse(
            userId = profile.id.toString(),
            email = profile.email,
            displayName = profile.displayName,
            currency = profile.currency,
            timezone = profile.timezone,
        )
    }
}
