package com.finpilot.data.db

import java.sql.SQLException
import javax.sql.DataSource

class DatabaseHealth(private val dataSource: DataSource) {

    /** "ok" when a connection can be obtained and `SELECT 1` succeeds, otherwise "down". */
    fun status(): String = try {
        dataSource.connection.use { connection ->
            connection.createStatement().use { it.execute("SELECT 1") }
        }
        "ok"
    } catch (_: SQLException) {
        "down"
    }
}
