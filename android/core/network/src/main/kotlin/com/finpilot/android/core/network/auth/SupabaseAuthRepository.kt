package com.finpilot.android.core.network.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SupabaseAuthRepository(
    private val client: SupabaseClient,
    scope: CoroutineScope,
) : AuthRepository {

    override val sessionState: StateFlow<SessionState> = client.auth.sessionStatus
        .map { status -> status.toSessionState() }
        .stateIn(scope, SharingStarted.Eagerly, SessionState.Loading)

    override suspend fun signIn(email: String, password: String): AuthResult = authCall {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        AuthResult.Success
    }

    override suspend fun signUp(email: String, password: String): AuthResult = authCall {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        if (client.auth.currentSessionOrNull() == null) AuthResult.ConfirmationRequired else AuthResult.Success
    }

    override suspend fun signOut() {
        runCatching { client.auth.signOut() }.onFailure { if (it is CancellationException) throw it }
    }

    override suspend fun accessToken(): String? = client.auth.currentSessionOrNull()?.accessToken

    override suspend fun refreshSession(): String? = runCatching {
        client.auth.refreshCurrentSession()
        client.auth.currentSessionOrNull()?.accessToken
    }.getOrElse { if (it is CancellationException) throw it else null }

    private suspend fun authCall(block: suspend () -> AuthResult): AuthResult = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (rest: RestException) {
        AuthResult.Failure(rest.description ?: rest.error)
    } catch (_: HttpRequestException) {
        AuthResult.Failure("Cannot reach the sign-in service. Check your connection and try again.")
    }

    private fun SessionStatus.toSessionState(): SessionState = when (this) {
        is SessionStatus.Authenticated -> SessionState.SignedIn(
            userId = session.user?.id.orEmpty(),
            email = session.user?.email,
        )
        is SessionStatus.NotAuthenticated -> SessionState.SignedOut
        is SessionStatus.Initializing -> SessionState.Loading
        // A failed background refresh keeps the current session until it really expires.
        is SessionStatus.RefreshFailure -> client.auth.currentSessionOrNull()?.let { session ->
            SessionState.SignedIn(userId = session.user?.id.orEmpty(), email = session.user?.email)
        } ?: SessionState.SignedOut
    }
}
