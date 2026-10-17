package com.finpilot.data

import com.finpilot.data.db.DatabaseMigrator
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer

/**
 * One PostgreSQL 17 database for integration tests across modules.
 *
 * Uses `TEST_DATABASE_URL` (+ `TEST_DATABASE_USER` / `TEST_DATABASE_PASSWORD`) when set — the Compose database
 * locally or the service container in CI — and otherwise starts a Testcontainers instance (needs Docker).
 * The connecting user must own the schema (it runs migrations and sets the API role's password).
 */
object TestDatabase {

    const val API_ROLE = "finpilot_api"

    /**
     * Roles are cluster-wide, so the tests (re)set the API role's password. Using the documented Compose
     * dev password keeps a local backend working after a test run; CI creates the role fresh anyway.
     */
    const val API_PASSWORD = "finpilot_api_dev_password"

    private val container: PostgreSQLContainer<*>? by lazy {
        if (System.getenv("TEST_DATABASE_URL") != null) {
            null
        } else {
            PostgreSQLContainer("postgres:17-alpine")
                .withDatabaseName("finpilot_test")
                .withUsername("finpilot_migrator")
                .withPassword("finpilot_migrator_test_password")
                .also { it.start() }
        }
    }

    val jdbcUrl: String get() = System.getenv("TEST_DATABASE_URL") ?: container!!.jdbcUrl
    val adminUser: String get() = System.getenv("TEST_DATABASE_USER") ?: container!!.username
    val adminPassword: String get() = System.getenv("TEST_DATABASE_PASSWORD") ?: container!!.password

    val adminDataSource: DataSource by lazy { dataSource(adminUser, adminPassword, poolSize = 2) }
    val apiDataSource: DataSource by lazy { dataSource(API_ROLE, API_PASSWORD, poolSize = 2) }

    /** Drops everything and re-applies all migrations, then gives the API role a password so tests can log in as it. */
    fun migrateFresh() {
        Flyway.configure()
            .dataSource(adminDataSource)
            .locations("classpath:db/migration")
            .cleanDisabled(false)
            .load()
            .clean()
        DatabaseMigrator(adminDataSource).migrate()
        asAdmin { it.update("ALTER ROLE $API_ROLE WITH LOGIN PASSWORD '$API_PASSWORD'") }
    }

    /** Runs [block] inside one transaction as the API role, with the RLS user context set (or deliberately absent). */
    fun <T> asApi(userId: UUID?, block: (Connection) -> T): T = apiDataSource.connection.use { connection ->
        connection.autoCommit = false
        runCatching {
            userId?.let { connection.setUserContext(it) }
            block(connection)
        }.onSuccess { connection.commit() }
            .onFailure { connection.rollback() }
            .getOrThrow()
    }

    fun <T> asAdmin(block: (Connection) -> T): T = adminDataSource.connection.use(block)

    private fun Connection.setUserContext(userId: UUID) {
        prepareStatement("SELECT set_config('app.current_user_id', ?, true)").use { statement ->
            statement.setString(1, userId.toString())
            statement.execute()
        }
    }

    private fun dataSource(user: String, password: String, poolSize: Int): DataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = this@TestDatabase.jdbcUrl
            username = user
            this.password = password
            maximumPoolSize = poolSize
            isAutoCommit = true
        },
    )
}

fun <T> Connection.query(sql: String, map: (ResultSet) -> T): List<T> =
    createStatement().use { statement ->
        statement.executeQuery(sql).use { rs ->
            buildList { while (rs.next()) add(map(rs)) }
        }
    }

fun Connection.count(sql: String): Int = query(sql) { it.getInt(1) }.single()

fun Connection.update(sql: String): Int = createStatement().use { it.executeUpdate(sql) }
