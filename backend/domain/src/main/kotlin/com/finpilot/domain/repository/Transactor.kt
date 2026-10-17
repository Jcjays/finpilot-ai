package com.finpilot.domain.repository

import java.util.UUID

/**
 * Runs repository work inside one database transaction scoped to a user.
 *
 * Implementations set the Row Level Security context for [userId] before [block] runs, so every
 * repository call inside the block can only see and change that user's rows — even if a query
 * forgets its `user_id` filter. Repositories must only ever be used inside [withUser].
 */
interface Transactor {
    suspend fun <T> withUser(userId: UUID, block: () -> T): T
}
