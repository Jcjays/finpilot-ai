package com.finpilot.android.core.network.auth

import kotlinx.coroutines.flow.StateFlow

/** Authentication facade over Supabase Auth. Tokens never leave this layer except through the API client. */
interface AuthRepository {
    val sessionState: StateFlow<SessionState>

    suspend fun signIn(email: String, password: String): AuthResult

    suspend fun signUp(email: String, password: String): AuthResult

    suspend fun signOut()

    /** Current access token, or null when signed out. */
    suspend fun accessToken(): String?

    /** Forces a refresh and returns the new access token, or null when the session cannot be renewed. */
    suspend fun refreshSession(): String?
}
