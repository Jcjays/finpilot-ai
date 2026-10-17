package com.finpilot.data.db

import com.finpilot.domain.repository.Transactor
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Exposed-backed [Transactor]. Each call opens one JDBC transaction on the IO dispatcher and sets the
 * transaction-local `app.current_user_id` setting that every Row Level Security policy reads.
 * `set_config(..., true)` is scoped to the transaction, so the context never leaks to the next
 * borrower of the pooled connection.
 */
class ExposedTransactor(private val database: Database) : Transactor {

    override suspend fun <T> withUser(userId: UUID, block: () -> T): T = withContext(Dispatchers.IO) {
        transaction(database) {
            // UUID.toString() has a fixed 36-character hex/dash format, so interpolating it cannot inject SQL.
            exec("SELECT set_config('app.current_user_id', '$userId', true)")
            block()
        }
    }
}
