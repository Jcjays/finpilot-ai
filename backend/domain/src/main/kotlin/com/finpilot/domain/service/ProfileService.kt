package com.finpilot.domain.service

import com.finpilot.domain.model.Profile
import com.finpilot.domain.repository.ProfileRepository
import com.finpilot.domain.repository.Transactor
import java.util.UUID

class ProfileService(
    private val transactor: Transactor,
    private val profiles: ProfileRepository,
) {
    /** Called on every authenticated request that needs the profile; creates it lazily on first sign-in. */
    suspend fun ensureProfile(userId: UUID, email: String?): Profile =
        transactor.withUser(userId) { profiles.upsert(userId, email) }
}
