package com.finpilot.android.feature.auth

import com.finpilot.android.core.network.auth.AuthRepository
import com.finpilot.android.core.network.auth.AuthResult
import com.finpilot.android.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : MviViewModel<SignInState, SignInIntent, SignInEffect>(SignInState()) {

    override suspend fun handle(intent: SignInIntent) {
        when (intent) {
            is SignInIntent.EmailChanged -> reduce { copy(email = intent.value, error = null) }
            is SignInIntent.PasswordChanged -> reduce { copy(password = intent.value, error = null) }
            SignInIntent.ToggleMode -> reduce {
                copy(mode = if (mode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN, error = null)
            }
            SignInIntent.Submit -> submit()
        }
    }

    private suspend fun submit() {
        val current = state.value
        if (!current.canSubmit) return
        reduce { copy(isSubmitting = true, error = null) }

        val email = current.email.trim()
        val result = when (current.mode) {
            AuthMode.SIGN_IN -> authRepository.signIn(email, current.password)
            AuthMode.SIGN_UP -> authRepository.signUp(email, current.password)
        }

        when (result) {
            AuthResult.Success -> {
                reduce { copy(isSubmitting = false) }
                emit(SignInEffect.NavigateToDashboard)
            }
            AuthResult.ConfirmationRequired -> {
                reduce { copy(isSubmitting = false, mode = AuthMode.SIGN_IN) }
                emit(SignInEffect.ShowMessage(CONFIRMATION_MESSAGE))
            }
            is AuthResult.Failure -> {
                reduce { copy(isSubmitting = false, error = result.message) }
                emit(SignInEffect.ShowMessage(result.message))
            }
        }
    }

    private companion object {
        const val CONFIRMATION_MESSAGE = "Check your inbox to confirm your email, then sign in."
    }
}
