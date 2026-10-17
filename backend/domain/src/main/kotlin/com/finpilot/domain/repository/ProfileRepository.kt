package com.finpilot.domain.repository

import com.finpilot.domain.model.Profile
import java.util.UUID

/** Profile persistence. Must be called inside [Transactor.withUser] for the same user. */
interface ProfileRepository {
    /** Creates the profile on first sign-in; later calls refresh the email and keep user-edited fields. */
    fun upsert(id: UUID, email: String?): Profile

    fun findById(id: UUID): Profile?
}
