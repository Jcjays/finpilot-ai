package com.finpilot.data.repository

import com.finpilot.data.tables.Profiles
import com.finpilot.domain.model.Profile
import com.finpilot.domain.repository.ProfileRepository
import java.util.UUID
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert

class ExposedProfileRepository : ProfileRepository {

    override fun upsert(id: UUID, email: String?): Profile {
        val key = id.toKotlinUuid()
        if (email == null) {
            // Nothing to refresh: create the row if missing, never blank out a stored email.
            Profiles.insertIgnore { it[Profiles.id] = key }
        } else {
            Profiles.upsert(Profiles.id) {
                it[Profiles.id] = key
                it[Profiles.email] = email
            }
        }
        return checkNotNull(findById(id)) { "profile $id vanished inside its own transaction" }
    }

    override fun findById(id: UUID): Profile? =
        Profiles.selectAll().where { Profiles.id eq id.toKotlinUuid() }.singleOrNull()?.toProfile()

    private fun ResultRow.toProfile() = Profile(
        id = this[Profiles.id].toJavaUuid(),
        email = this[Profiles.email],
        displayName = this[Profiles.displayName],
        currency = this[Profiles.currency].trim(),
        timezone = this[Profiles.timezone],
    )
}
