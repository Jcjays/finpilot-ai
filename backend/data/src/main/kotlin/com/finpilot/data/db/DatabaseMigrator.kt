package com.finpilot.data.db

import javax.sql.DataSource
import org.flywaydb.core.Flyway

/**
 * Applies the versioned SQL migrations under `db/migration`. Runs with the schema-owner role
 * (`finpilot_migrator` locally, `postgres` on Supabase), never with the API role.
 */
class DatabaseMigrator(private val dataSource: DataSource) {

    /** Applies pending migrations and returns how many were executed. */
    fun migrate(): Int = Flyway.configure()
        .dataSource(dataSource)
        .locations(MIGRATIONS_LOCATION)
        .validateMigrationNaming(true)
        .load()
        .migrate()
        .migrationsExecuted

    private companion object {
        const val MIGRATIONS_LOCATION = "classpath:db/migration"
    }
}
