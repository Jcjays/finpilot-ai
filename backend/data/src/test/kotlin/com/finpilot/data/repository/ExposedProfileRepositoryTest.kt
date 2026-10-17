package com.finpilot.data.repository

import com.finpilot.data.TestDatabase
import com.finpilot.data.count
import com.finpilot.data.db.ExposedTransactor
import com.finpilot.data.update
import com.finpilot.domain.model.Profile
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database

class ExposedProfileRepositoryTest {

    private val database = Database.connect(TestDatabase.apiDataSource)
    private val transactor = ExposedTransactor(database)
    private val repository = ExposedProfileRepository()
    private val alice = UUID.randomUUID()
    private val bob = UUID.randomUUID()

    @BeforeTest
    fun freshDatabase() = TestDatabase.migrateFresh()

    @Test
    fun upsertCreatesAProfileWithPhilippineDefaults() = runBlocking {
        val profile = transactor.withUser(alice) { repository.upsert(alice, "alice@example.test") }

        assertEquals(Profile(id = alice, email = "alice@example.test"), profile)
        assertEquals("PHP", profile.currency)
        assertEquals("Asia/Manila", profile.timezone)
    }

    @Test
    fun upsertRefreshesTheEmailButKeepsWhatTheUserCustomised() = runBlocking {
        transactor.withUser(alice) { repository.upsert(alice, "old@example.test") }
        TestDatabase.asAdmin { it.update("UPDATE profiles SET display_name = 'Chris' WHERE id = '$alice'") }

        val updated = transactor.withUser(alice) { repository.upsert(alice, "new@example.test") }

        assertEquals("new@example.test", updated.email)
        assertEquals("Chris", updated.displayName)
    }

    @Test
    fun aUserCannotReadAnotherUsersProfile() = runBlocking {
        transactor.withUser(alice) { repository.upsert(alice, "alice@example.test") }
        transactor.withUser(bob) { repository.upsert(bob, "bob@example.test") }

        val seenByAlice = transactor.withUser(alice) { repository.findById(bob) }
        val own = transactor.withUser(bob) { repository.findById(bob) }

        assertNull(seenByAlice)
        assertEquals("bob@example.test", own?.email)
    }

    @Test
    fun theUserContextDoesNotLeakIntoLaterConnections() = runBlocking {
        transactor.withUser(alice) { repository.upsert(alice, "alice@example.test") }

        val visibleWithoutContext = TestDatabase.asApi(userId = null) { it.count("SELECT count(*) FROM profiles") }

        assertEquals(0, visibleWithoutContext)
    }
}
