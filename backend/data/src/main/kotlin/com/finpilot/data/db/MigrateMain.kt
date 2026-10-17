package com.finpilot.data.db

import com.zaxxer.hikari.HikariDataSource

/**
 * Command-line entry point behind `./gradlew :data:migrate`. Runs migrations with the schema-owner
 * credentials from the environment, which are never given to the running API.
 */
object MigrateMain {

    @JvmStatic
    fun main(args: Array<String>) {
        val settings = settingsFromEnv()
        val applied = run(settings)
        println("Applied $applied migration(s) to ${settings.jdbcUrl} as ${settings.user}")
    }

    /**
     * Applies pending migrations and returns how many were executed. Flyway holds the schema-history lock on one
     * connection while applying migrations on another, so the pool needs at least two connections.
     */
    fun run(settings: DatabaseSettings): Int {
        val pooled = settings.copy(poolSize = maxOf(settings.poolSize, MIN_MIGRATION_CONNECTIONS))
        return (DatabaseFactory.dataSource(pooled) as HikariDataSource).use { DatabaseMigrator(it).migrate() }
    }

    private const val MIN_MIGRATION_CONNECTIONS = 2

    fun settingsFromEnv(env: Map<String, String> = System.getenv()): DatabaseSettings {
        val url = env["MIGRATION_DATABASE_URL"]?.ifBlank { null }
            ?: env["DATABASE_URL"]?.ifBlank { null }
            ?: error("Set MIGRATION_DATABASE_URL (or DATABASE_URL) to the JDBC URL of the database to migrate.")
        val user = env["MIGRATION_DATABASE_USER"]?.ifBlank { null }
            ?: error(
                "Set MIGRATION_DATABASE_USER to the schema-owner role " +
                    "(finpilot_migrator locally, postgres.<ref> on Supabase).",
            )
        val password = env["MIGRATION_DATABASE_PASSWORD"]?.ifBlank { null }
            ?: error("Set MIGRATION_DATABASE_PASSWORD for $user.")
        return DatabaseSettings(jdbcUrl = url, user = user, password = password, poolSize = MIN_MIGRATION_CONNECTIONS)
    }
}
