package com.finpilot.domain.service

import com.finpilot.domain.model.Profile
import com.finpilot.domain.repository.ProfileRepository
import com.finpilot.domain.repository.Transactor
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class ProfileServiceTest {

    private class RecordingTransactor : Transactor {
        val scopes = mutableListOf<UUID>()
        var active: UUID? = null

        override suspend fun <T> withUser(userId: UUID, block: () -> T): T {
            scopes += userId
            active = userId
            return try {
                block()
            } finally {
                active = null
            }
        }
    }

    private class InMemoryProfiles(private val transactor: RecordingTransactor) : ProfileRepository {
        val stored = mutableMapOf<UUID, Profile>()

        override fun upsert(id: UUID, email: String?): Profile {
            check(transactor.active == id) { "repository used outside the owner's transaction scope" }
            val profile = stored[id]?.copy(email = email ?: stored[id]?.email) ?: Profile(id = id, email = email)
            stored[id] = profile
            return profile
        }

        override fun findById(id: UUID): Profile? = stored[id].takeIf { transactor.active == id }
    }

    private val transactor = RecordingTransactor()
    private val repository = InMemoryProfiles(transactor)
    private val service = ProfileService(transactor, repository)

    @Test
    fun ensureProfileRunsInsideTheCallersScopeAndReturnsTheProfile() = runTest {
        val userId = UUID.randomUUID()

        val profile = service.ensureProfile(userId, "chris@example.test")

        assertEquals(listOf(userId), transactor.scopes)
        assertEquals(Profile(id = userId, email = "chris@example.test"), profile)
        assertEquals("PHP", profile.currency)
        assertEquals("Asia/Manila", profile.timezone)
    }

    @Test
    fun ensureProfileIsIdempotentForRepeatedSignIns() = runTest {
        val userId = UUID.randomUUID()

        service.ensureProfile(userId, "chris@example.test")
        val second = service.ensureProfile(userId, "chris@example.test")

        assertEquals(1, repository.stored.size)
        assertEquals("chris@example.test", second.email)
    }
}
