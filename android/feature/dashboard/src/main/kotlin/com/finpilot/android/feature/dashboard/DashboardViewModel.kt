package com.finpilot.android.feature.dashboard

import com.finpilot.android.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel(
    monthLabelProvider: () -> String,
) : MviViewModel<DashboardState, DashboardIntent, DashboardEffect>(
    DashboardState(monthLabel = monthLabelProvider()),
) {

    @Inject
    constructor() : this(monthLabelProvider = ::currentManilaMonthLabel)

    override suspend fun handle(intent: DashboardIntent) {
        when (intent) {
            DashboardIntent.Refresh -> refresh()
        }
    }

    private suspend fun refresh() {
        reduce { copy(isRefreshing = true) }
        emit(DashboardEffect.ShowMessage(PLACEHOLDER_MESSAGE))
        reduce { copy(isRefreshing = false) }
    }

    private companion object {
        const val PLACEHOLDER_MESSAGE = "Dashboard data arrives in Milestone 3"
    }
}

private val manilaZone: ZoneId = ZoneId.of("Asia/Manila")
private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

private fun currentManilaMonthLabel(): String = YearMonth.now(manilaZone).format(monthFormatter)
