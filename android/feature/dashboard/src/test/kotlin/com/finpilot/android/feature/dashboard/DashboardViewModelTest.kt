package com.finpilot.android.feature.dashboard

import app.cash.turbine.test
import com.finpilot.android.core.network.auth.AuthRepository
import com.finpilot.android.core.network.auth.AuthResult
import com.finpilot.android.core.network.auth.SessionState
import com.finpilot.android.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeAuthRepository : AuthRepository {
        override val sessionState = MutableStateFlow<SessionState>(
            SessionState.SignedIn(userId = "user-1", email = "chris@example.test"),
        )
        var signOutCalls = 0
        override suspend fun signIn(email: String, password: String): AuthResult = AuthResult.Success
        override suspend fun signUp(email: String, password: String): AuthResult = AuthResult.Success
        override suspend fun signOut() {
            signOutCalls++
            sessionState.value = SessionState.SignedOut
        }
        override suspend fun accessToken(): String? = "token"
        override suspend fun refreshSession(): String? = "token"
    }

    private val authRepository = FakeAuthRepository()
    private val viewModel = DashboardViewModel(authRepository, monthLabelProvider = { "October 2026" })

    @Test
    fun startsIdleWithTheCurrentMonthLabel() {
        assertEquals(DashboardState(monthLabel = "October 2026", isRefreshing = false), viewModel.state.value)
    }

    @Test
    fun refreshFinishesAndTellsTheUserDataIsNotWiredYet() = runTest {
        viewModel.effects.test {
            viewModel.dispatch(DashboardIntent.Refresh)

            assertEquals(DashboardEffect.ShowMessage("Dashboard data arrives in Milestone 3"), awaitItem())
            assertFalse(viewModel.state.value.isRefreshing)
            expectNoEvents()
        }
    }

    @Test
    fun signOutEndsTheSession() = runTest {
        viewModel.dispatch(DashboardIntent.SignOut)

        assertEquals(1, authRepository.signOutCalls)
        assertEquals(SessionState.SignedOut, authRepository.sessionState.value)
    }
}
