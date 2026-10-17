package com.finpilot.api.auth

import java.util.UUID

/** The authenticated caller, derived from a verified Supabase access token. `userId` is the JWT `sub`. */
data class UserPrincipal(
    val userId: UUID,
    val email: String?,
)
