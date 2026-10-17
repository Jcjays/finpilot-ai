package com.finpilot.android.core.network.auth

sealed interface AuthResult {
    data object Success : AuthResult

    /** Sign-up succeeded but the project requires email confirmation before the first sign-in. */
    data object ConfirmationRequired : AuthResult

    data class Failure(val message: String) : AuthResult
}
