package com.finpilot.android

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.finpilot.android.core.ui.navigation.Route
import com.finpilot.android.feature.dashboard.DashboardRoute

/**
 * Single Navigation 3 host. Each feature contributes its entries; the back stack is a list of
 * serializable [Route] keys so it survives configuration changes and process death.
 */
@Composable
fun FinPilotApp() {
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
