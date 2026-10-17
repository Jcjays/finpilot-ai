package com.finpilot.data.db

import com.finpilot.data.TestDatabase
import com.finpilot.data.count
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.flywaydb.core.Flyway

class MigrateMainTest {

    private val adminSettings = DatabaseSettings(
        jdbcUrl = TestDatabase.jdbcUrl,
        user = TestDatabase.adminUser,
        password = TestDatabase.adminPassword,
        poolSize = 1,
    )

    @Test
    fun appliesEveryMigrationToAnEmptyDatabaseAndThenHasNothingLeftToDo() {
        Flyway.configure().dataSource(TestDatabase.adminDataSource).cleanDisabled(false).load().clean()

        val firstRun = MigrateMain.run(adminSettings)
        val secondRun = MigrateMain.run(adminSettings)

        assertTrue(firstRun >= 2, "expected at least V1 and V2 to be applied, got $firstRun")
        assertEquals(0, secondRun)
        assertTrue(TestDatabase.asAdmin { it.count("SELECT count(*) FROM categories") } > 0)
    }

    @Test
    fun readsTheMigratorCredentialsFromTheEnvironment() {
        val settings = MigrateMain.settingsFromEnv(
            mapOf(
                "DATABASE_URL" to "jdbc:postgresql://localhost:5432/finpilot",
                "MIGRATION_DATABASE_USER" to "finpilot_migrator",
                "MIGRATION_DATABASE_PASSWORD" to "secret",
            ),
        )

        assertEquals("jdbc:postgresql://localhost:5432/finpilot", settings.jdbcUrl)
        assertEquals("finpilot_migrator", settings.user)
        assertEquals("secret", settings.password)
    }

    @Test
    fun prefersADedicatedMigrationUrlWhenPresent() {
        val settings = MigrateMain.settingsFromEnv(
            mapOf(
                "DATABASE_URL" to "jdbc:postgresql://pooler:5432/postgres",
                "MIGRATION_DATABASE_URL" to "jdbc:postgresql://direct:5432/postgres",
                "MIGRATION_DATABASE_USER" to "postgres.ref",
                "MIGRATION_DATABASE_PASSWORD" to "secret",
            ),
        )

        assertEquals("jdbc:postgresql://direct:5432/postgres", settings.jdbcUrl)
    }

    @Test
    fun refusesToRunWithoutMigratorCredentials() {
        val failure = assertFailsWith<IllegalStateException> {
            MigrateMain.settingsFromEnv(mapOf("DATABASE_URL" to "jdbc:postgresql://localhost:5432/finpilot"))
        }

        assertTrue(failure.message.orEmpty().contains("MIGRATION_DATABASE_USER"), failure.message)
    }
}
