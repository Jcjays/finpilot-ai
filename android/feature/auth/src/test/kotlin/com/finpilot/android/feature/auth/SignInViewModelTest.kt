package com.finpilot.android.feature.auth

import app.cash.turbine.test
import com.finpilot.android.core.network.auth.AuthRepository
import com.finpilot.android.core.network.auth.AuthResult
import com.finpilot.android.core.network.auth.SessionState
import com.finpilot.android.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SignInViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeAuthRepository : AuthRepository {
        override val sessionState = MutableStateFlow<SessionState>(SessionState.SignedOut)
        val signIns = mutableListOf<Pair<String, String>>()
        val signUps = mutableListOf<Pair<String, String>>()
        var nextResult: AuthResult = AuthResult.Success

        override suspend fun signIn(email: String, password: String): AuthResult {
            signIns += email to password
            return nextResult
        }

        override suspend fun signUp(email: String, password: String): AuthResult {
            signUps += email to password
            return nextResult
        }

        override suspend fun signOut() {
            sessionState.value = SessionState.SignedOut
        }

        override suspend fun accessToken(): String? = null

        override suspend fun refreshSession(): String? = null
    }

    private val repository = FakeAuthRepository()
    private val viewModel = SignInViewModel(repository)

    @Test
    fun startsEmptyInSignInModeWithSubmitDisabled() {
        val state = viewModel.state.value

        assertEquals(AuthMode.SIGN_IN, state.mode)
        assertEquals("", state.email)
        assertFalse(state.canSubmit)
    }

    @Test
    fun enablesSubmitOnlyWhenEmailLooksValidAndPasswordIsLongEnough() {
        viewModel.dispatch(SignInIntent.EmailChanged("chris@example.test"))
        viewModel.dispatch(SignInIntent.PasswordChanged("short"))
        assertFalse(viewModel.state.value.canSubmit)

        viewModel.dispatch(SignInIntent.PasswordChanged("longenough1"))
        assertTrue(viewModel.state.value.canSubmit)

        viewModel.dispatch(SignInIntent.EmailChanged("not-an-email"))
        assertFalse(viewModel.state.value.canSubmit)
    }

    @Test
    fun successfulSignInNavigatesToTheDashboard() = runTest {
        viewModel.dispatch(SignInIntent.EmailChanged("chris@example.test"))
        viewModel.dispatch(SignInIntent.PasswordChanged("longenough1"))

        viewModel.effects.test {
            viewModel.dispatch(SignInIntent.Submit)

            assertEquals(SignInEffect.NavigateToDashboard, awaitItem())
            assertEquals(listOf("chris@example.test" to "longenough1"), repository.signIns)
            assertFalse(viewModel.state.value.isSubmitting)
            expectNoEvents()
        }
    }

    @Test
    fun failedSignInShowsTheErrorAndKeepsTheForm() = runTest {
        repository.nextResult = AuthResult.Failure("Invalid login credentials")
        viewModel.dispatch(SignInIntent.EmailChanged("chris@example.test"))
        viewModel.dispatch(SignInIntent.PasswordChanged("longenough1"))

        viewModel.effects.test {
            viewModel.dispatch(SignInIntent.Submit)

            assertEquals(SignInEffect.ShowMessage("Invalid login credentials"), awaitItem())
            assertEquals("chris@example.test", viewModel.state.value.email)
            assertEquals("Invalid login credentials", viewModel.state.value.error)
            assertFalse(viewModel.state.value.isSubmitting)
        }
    }

    @Test
    fun signUpModeCallsSignUpAndTellsTheUserToCheckTheirInbox() = runTest {
        repository.nextResult = AuthResult.ConfirmationRequired
        viewModel.dispatch(SignInIntent.ToggleMode)
        viewModel.dispatch(SignInIntent.EmailChanged("new@example.test"))
        viewModel.dispatch(SignInIntent.PasswordChanged("longenough1"))

        viewModel.effects.test {
            viewModel.dispatch(SignInIntent.Submit)

            assertEquals(listOf("new@example.test" to "longenough1"), repository.signUps)
            assertTrue(repository.signIns.isEmpty())
            assertEquals(SignInEffect.ShowMessage("Check your inbox to confirm your email, then sign in."), awaitItem())
            // After confirmation is requested the form switches to sign-in so the next step is one tap away.
            assertEquals(AuthMode.SIGN_IN, viewModel.state.value.mode)
            assertEquals("new@example.test", viewModel.state.value.email)
        }
    }

    @Test
    fun typingClearsAPreviousError() = runTest {
        repository.nextResult = AuthResult.Failure("Invalid login credentials")
        viewModel.dispatch(SignInIntent.EmailChanged("chris@example.test"))
        viewModel.dispatch(SignInIntent.PasswordChanged("longenough1"))
        viewModel.dispatch(SignInIntent.Submit)

        viewModel.dispatch(SignInIntent.PasswordChanged("longenough2"))

        assertNull(viewModel.state.value.error)
    }
}
