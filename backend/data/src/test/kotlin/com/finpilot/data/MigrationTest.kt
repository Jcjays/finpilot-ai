package com.finpilot.data

import java.sql.SQLException
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MigrationTest {

    private val userTables = listOf(
        "profiles", "accounts", "categories", "transactions", "budgets", "savings_goals",
        "savings_goal_contributions", "ai_insights", "ai_action_proposals", "audit_logs",
        "idempotency_keys", "receipt_extractions",
    )

    @BeforeTest
    fun freshDatabase() = TestDatabase.migrateFresh()

    @Test
    fun createsEveryTableFromTheApprovedSchema() {
        val tables = TestDatabase.asAdmin { connection ->
            connection.query("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'") {
                it.getString(1)
            }
        }.toSet()

        userTables.forEach { table -> assertTrue(table in tables, "missing table $table; found $tables") }
    }

    @Test
    fun seedsSystemCategoriesForThePhilippines() {
        val names = TestDatabase.asAdmin { connection ->
            connection.query("SELECT name FROM categories WHERE user_id IS NULL ORDER BY kind, sort_order") {
                it.getString(1)
            }
        }
        val expectedNames = listOf(
            "Food and Dining", "Groceries", "Transportation", "Utilities",
            "Remittance / Padala", "Salary", "13th Month Pay",
        )
        expectedNames.forEach { expected ->
            assertTrue(expected in names, "missing seed category $expected; seeded: $names")
        }

        val kinds = TestDatabase.asAdmin { connection ->
            connection.query("SELECT kind, count(*) FROM categories WHERE user_id IS NULL GROUP BY kind") {
                it.getString(1) to it.getInt(2)
            }
        }.toMap()
        assertTrue((kinds["EXPENSE"] ?: 0) >= 10, "expected at least 10 expense categories, got $kinds")
        assertTrue((kinds["INCOME"] ?: 0) >= 5, "expected at least 5 income categories, got $kinds")
    }

    @Test
    fun enablesAndForcesRowLevelSecurityOnEveryUserTable() {
        val rls = TestDatabase.asAdmin { connection ->
            connection.query(
                "SELECT relname, relrowsecurity, relforcerowsecurity FROM pg_class " +
                    "WHERE relnamespace = 'public'::regnamespace AND relkind = 'r'",
            ) { Triple(it.getString(1), it.getBoolean(2), it.getBoolean(3)) }
        }.associateBy { it.first }

        userTables.forEach { table ->
            val row = rls.getValue(table)
            assertTrue(row.second && row.third, "RLS must be enabled and forced on $table, got $row")
        }
    }

    @Test
    fun apiRoleCanReadAndWriteButNeverHardDeleteContent() {
        val privileges = TestDatabase.asAdmin { connection ->
            listOf("SELECT", "INSERT", "UPDATE", "DELETE").associateWith { privilege ->
                connection.query("SELECT has_table_privilege('finpilot_api', 'transactions', '$privilege')") {
                    it.getBoolean(1)
                }.single()
            }
        }

        assertEquals(mapOf("SELECT" to true, "INSERT" to true, "UPDATE" to true, "DELETE" to false), privileges)
    }

    @Test
    fun rejectsTransfersThatAreMissingTheirGroup() {
        val userId = UUID.randomUUID()
        TestDatabase.asAdmin { connection ->
            val accountId = connection.createUserWithCashAccount(userId)

            val failure = assertFailsWith<SQLException> {
                connection.insertTransaction(userId, accountId, type = "TRANSFER", amountMinor = 1000)
            }
            assertTrue(failure.message.orEmpty().contains("transactions_transfer"), failure.message)
        }
    }

    @Test
    fun rejectsZeroOrNegativeAmounts() {
        val userId = UUID.randomUUID()
        TestDatabase.asAdmin { connection ->
            val accountId = connection.createUserWithCashAccount(userId)

            assertFailsWith<SQLException> {
                connection.insertTransaction(userId, accountId, type = "EXPENSE", amountMinor = 0)
            }
        }
    }

    private fun java.sql.Connection.createUserWithCashAccount(userId: UUID): String {
        update("INSERT INTO profiles (id, email) VALUES ('$userId', 'a@example.test')")
        update("INSERT INTO accounts (user_id, name, type) VALUES ('$userId', 'Cash', 'CASH')")
        return query("SELECT id FROM accounts WHERE user_id = '$userId'") { it.getString(1) }.single()
    }

    private fun java.sql.Connection.insertTransaction(
        userId: UUID,
        accountId: String,
        type: String,
        amountMinor: Long,
    ) {
        update(
            "INSERT INTO transactions (user_id, account_id, type, status, amount_minor, occurred_at, occurred_on) " +
                "VALUES ('$userId', '$accountId', '$type', 'CLEARED', $amountMinor, now(), current_date)",
        )
    }
}
