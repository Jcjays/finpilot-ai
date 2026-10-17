package com.finpilot.android.feature.dashboard

import com.finpilot.android.core.ui.mvi.UiEffect
import com.finpilot.android.core.ui.mvi.UiIntent
import com.finpilot.android.core.ui.mvi.UiState

data class DashboardState(
    val monthLabel: String,
    val isRefreshing: Boolean = false,
) : UiState

sealed interface DashboardIntent : UiIntent {
    data object Refresh : DashboardIntent
    data object SignOut : DashboardIntent
}

sealed interface DashboardEffect : UiEffect {
    data class ShowMessage(val text: String) : DashboardEffect
}
