package com.finpilot.android.feature.auth

import com.finpilot.android.core.ui.mvi.UiEffect
import com.finpilot.android.core.ui.mvi.UiIntent
import com.finpilot.android.core.ui.mvi.UiState

enum class AuthMode { SIGN_IN, SIGN_UP }

data class SignInState(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
) : UiState {
    val isEmailValid: Boolean get() = EMAIL_PATTERN.matches(email.trim())
    val isPasswordValid: Boolean get() = password.length >= MIN_PASSWORD_LENGTH
    val canSubmit: Boolean get() = !isSubmitting && isEmailValid && isPasswordValid

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
        private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}

sealed interface SignInIntent : UiIntent {
    data class EmailChanged(val value: String) : SignInIntent
    data class PasswordChanged(val value: String) : SignInIntent
    data object ToggleMode : SignInIntent
    data object Submit : SignInIntent
}

sealed interface SignInEffect : UiEffect {
    data object NavigateToDashboard : SignInEffect
    data class ShowMessage(val text: String) : SignInEffect
}
