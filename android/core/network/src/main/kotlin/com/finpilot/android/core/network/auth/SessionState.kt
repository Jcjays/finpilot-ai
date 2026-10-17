package com.finpilot.android.core.network.auth

sealed interface SessionState {
    /** Session storage is still being read; show a splash rather than the sign-in form. */
    data object Loading : SessionState

    data object SignedOut : SessionState

    data class SignedIn(val userId: String, val email: String?) : SessionState
}
