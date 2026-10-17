package com.finpilot.domain.model

import java.util.UUID

/** A user's profile row. `id` is the Supabase auth user id. */
data class Profile(
    val id: UUID,
    val email: String? = null,
    val displayName: String? = null,
    val currency: String = DEFAULT_CURRENCY,
    val timezone: String = DEFAULT_TIMEZONE,
) {
    companion object {
        const val DEFAULT_CURRENCY = "PHP"
        const val DEFAULT_TIMEZONE = "Asia/Manila"
    }
}
