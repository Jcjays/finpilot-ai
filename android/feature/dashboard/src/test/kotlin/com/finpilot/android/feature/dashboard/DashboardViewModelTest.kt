package com.finpilot.android.feature.dashboard

import app.cash.turbine.test
import com.finpilot.android.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun startsIdleWithTheCurrentMonthLabel() {
        val viewModel = DashboardViewModel(monthLabelProvider = { "October 2026" })

        assertEquals(DashboardState(monthLabel = "October 2026", isRefreshing = false), viewModel.state.value)
    }

    @Test
    fun refreshFinishesAndTellsTheUserDataIsNotWiredYet() = runTest {
        val viewModel = DashboardViewModel(monthLabelProvider = { "October 2026" })

        viewModel.effects.test {
            viewModel.dispatch(DashboardIntent.Refresh)

            assertEquals(DashboardEffect.ShowMessage("Dashboard data arrives in Milestone 3"), awaitItem())
            assertFalse(viewModel.state.value.isRefreshing)
            expectNoEvents()
        }
    }
}
