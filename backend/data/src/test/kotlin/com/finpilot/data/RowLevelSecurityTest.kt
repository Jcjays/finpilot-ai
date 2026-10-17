package com.finpilot.data

import java.sql.SQLException
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Connects as the least-privilege `finpilot_api` role and proves that Row Level Security isolates users
 * even when the application forgets to filter by user_id.
 */
class RowLevelSecurityTest {

    private val alice = UUID.randomUUID()
    private val bob = UUID.randomUUID()

    @BeforeTest
    fun freshDatabaseWithTwoUsers() {
        TestDatabase.migrateFresh()
        TestDatabase.asAdmin { connection ->
            connection.update("INSERT INTO profiles (id, email) VALUES ('$alice', 'alice@example.test')")
            connection.update("INSERT INTO profiles (id, email) VALUES ('$bob', 'bob@example.test')")
        }
        TestDatabase.asApi(bob) { connection ->
            connection.update("INSERT INTO accounts (user_id, name, type) VALUES ('$bob', 'Bob wallet', 'EWALLET')")
        }
    }

    @Test
    fun aUserOnlySeesTheirOwnRowsEvenWithoutAWhereClause() {
        val seenByAlice = TestDatabase.asApi(alice) { it.count("SELECT count(*) FROM accounts") }
        val seenByBob = TestDatabase.asApi(bob) { it.count("SELECT count(*) FROM accounts") }

        assertEquals(0, seenByAlice)
        assertEquals(1, seenByBob)
    }

    @Test
    fun nothingIsVisibleWhenNoUserContextIsSet() {
        val seen = TestDatabase.asApi(userId = null) { it.count("SELECT count(*) FROM accounts") }

        assertEquals(0, seen)
    }

    @Test
    fun aUserCannotInsertRowsOwnedByAnotherUser() {
        val failure = assertFailsWith<SQLException> {
            TestDatabase.asApi(alice) { connection ->
                connection.update("INSERT INTO accounts (user_id, name, type) VALUES ('$bob', 'Smuggled', 'CASH')")
            }
        }

        assertTrue(failure.message.orEmpty().contains("row-level security"), failure.message)
    }

    @Test
    fun aUserCannotUpdateAnotherUsersRows() {
        val updated = TestDatabase.asApi(alice) { connection ->
            connection.update("UPDATE accounts SET name = 'Hijacked' WHERE user_id = '$bob'")
        }
        val bobName = TestDatabase.asApi(bob) { connection ->
            connection.query("SELECT name FROM accounts") { rs -> rs.getString(1) }.single()
        }

        assertEquals(0, updated)
        assertEquals("Bob wallet", bobName)
    }

    @Test
    fun systemCategoriesAreVisibleToEveryoneButOwnedByNobody() {
        val systemCount = TestDatabase.asApi(alice) { connection ->
            connection.count("SELECT count(*) FROM categories WHERE user_id IS NULL")
        }
        val failure = assertFailsWith<SQLException> {
            TestDatabase.asApi(alice) { connection ->
                connection.update("UPDATE categories SET name = 'Renamed' WHERE user_id IS NULL")
                connection.update("INSERT INTO categories (user_id, name, kind) VALUES (NULL, 'Global', 'EXPENSE')")
            }
        }

        assertTrue(systemCount > 0)
        assertTrue(failure.message.orEmpty().contains("row-level security"), failure.message)
    }

    @Test
    fun auditLogIsAppendOnlyForTheApiRole() {
        TestDatabase.asApi(alice) { connection ->
            connection.update(
                "INSERT INTO audit_logs (user_id, actor, action, entity_type, entity_id) " +
                    "VALUES ('$alice', 'USER', 'test.append', 'account', '${UUID.randomUUID()}')",
            )
        }

        val updateFailure = assertFailsWith<SQLException> {
            TestDatabase.asApi(alice) { it.update("UPDATE audit_logs SET action = 'tampered'") }
        }
        val deleteFailure = assertFailsWith<SQLException> {
            TestDatabase.asApi(alice) { it.update("DELETE FROM audit_logs") }
        }

        assertTrue(updateFailure.message.orEmpty().contains("permission denied"), updateFailure.message)
        assertTrue(deleteFailure.message.orEmpty().contains("permission denied"), deleteFailure.message)
    }

    @Test
    fun theApiRoleCannotBypassRowLevelSecurity() {
        val bypass = TestDatabase.asAdmin {
            it.query("SELECT rolbypassrls FROM pg_roles WHERE rolname = 'finpilot_api'") { rs -> rs.getBoolean(1) }
                .single()
        }

        assertEquals(false, bypass)
    }
}
