package com.finpilot.data.db

/** Connection settings for the API role (never the migrator). */
data class DatabaseSettings(
    val jdbcUrl: String,
    val user: String,
    val password: String,
    val poolSize: Int = DEFAULT_POOL_SIZE,
) {
    companion object {
        /** Small on purpose: Supabase's shared session pooler has tight per-project connection limits. */
        const val DEFAULT_POOL_SIZE = 4
    }
}
