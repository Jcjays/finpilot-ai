package com.finpilot.android.feature.dashboard

import app.cash.turbine.test
import com.finpilot.android.core.network.api.ApiEnvelope
import com.finpilot.android.core.network.api.FinPilotApi
import com.finpilot.android.core.network.api.MeDto
import com.finpilot.android.core.network.auth.AuthRepository
import com.finpilot.android.core.network.auth.AuthResult
import com.finpilot.android.core.network.auth.SessionState
import com.finpilot.android.core.testing.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    private class FakeFinPilotApi : FinPilotApi {
        var profile = MeDto(userId = "user-1", email = "chris@example.test", currency = "PHP", timezone = "Asia/Manila")
        var failure: Throwable? = null
        var calls = 0

        override suspend fun me(): ApiEnvelope<MeDto> {
            calls++
            failure?.let { throw it }
            return ApiEnvelope(profile)
        }
    }

    private val authRepository = FakeAuthRepository()
    private val api = FakeFinPilotApi()

    private fun viewModel() = DashboardViewModel(authRepository, api, monthLabelProvider = { "October 2026" })

    @Test
    fun loadsTheSignedInProfileThroughTheApiOnStart() {
        val viewModel = viewModel()

        val state = viewModel.state.value
        assertEquals("October 2026", state.monthLabel)
        assertEquals("chris@example.test", state.profileEmail)
        assertEquals("PHP", state.currency)
        assertFalse(state.isRefreshing)
        assertEquals(1, api.calls)
    }

    @Test
    fun refreshReloadsTheProfile() = runTest {
        val viewModel = viewModel()
        api.profile = api.profile.copy(email = "renamed@example.test")

        viewModel.dispatch(DashboardIntent.Refresh)

        assertEquals("renamed@example.test", viewModel.state.value.profileEmail)
        assertEquals(2, api.calls)
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun tellsTheUserWhenTheProfileCannotBeLoaded() = runTest {
        api.failure = IOException("offline")

        val viewModel = viewModel()

        viewModel.effects.test {
            val expected = DashboardEffect.ShowMessage("Could not load your profile. Tap Refresh to try again.")
            assertEquals(expected, awaitItem())
            assertNull(viewModel.state.value.profileEmail)
            assertFalse(viewModel.state.value.isRefreshing)
        }
    }

    @Test
    fun signOutEndsTheSession() = runTest {
        val viewModel = viewModel()

        viewModel.dispatch(DashboardIntent.SignOut)

        assertEquals(1, authRepository.signOutCalls)
        assertEquals(SessionState.SignedOut, authRepository.sessionState.value)
    }
}
