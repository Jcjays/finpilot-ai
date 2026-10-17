package com.finpilot.data.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import org.jetbrains.exposed.v1.jdbc.Database

object DatabaseFactory {

    private const val CONNECTION_TIMEOUT_MS = 5_000L
    private const val VALIDATION_TIMEOUT_MS = 3_000L

    fun dataSource(settings: DatabaseSettings): DataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = settings.jdbcUrl
            username = settings.user
            password = settings.password
            maximumPoolSize = settings.poolSize
            poolName = "finpilot-api"
            connectionTimeout = CONNECTION_TIMEOUT_MS
            validationTimeout = VALIDATION_TIMEOUT_MS
        },
    )

    fun connect(dataSource: DataSource): Database = Database.connect(dataSource)
}
