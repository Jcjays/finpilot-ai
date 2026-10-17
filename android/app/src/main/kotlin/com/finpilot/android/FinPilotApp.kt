package com.finpilot.android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.finpilot.android.core.network.auth.SessionState
import com.finpilot.android.core.ui.navigation.Route
import com.finpilot.android.feature.auth.SignInRoute
import com.finpilot.android.feature.dashboard.DashboardRoute

/**
 * Root of the UI. The Supabase session decides what is shown: a splash while the stored session loads,
 * the sign-in screen when signed out, and the authenticated Navigation 3 graph otherwise.
 */
@Composable
fun FinPilotApp(sessionViewModel: SessionViewModel = hiltViewModel()) {
    val session by sessionViewModel.sessionState.collectAsStateWithLifecycle()

    when (session) {
        SessionState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        SessionState.SignedOut -> SignInRoute(onSignedIn = { /* the session flow switches the graph */ })
        is SessionState.SignedIn -> AuthenticatedGraph()
    }
}

@Composable
private fun AuthenticatedGraph() {
    val backStack = rememberNavBackStack(Route.Dashboard)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Route.Dashboard> {
                DashboardRoute()
            }
        },
    )
}
