package com.finpilot.android.core.ui.mvi

import app.cash.turbine.test
import com.finpilot.android.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MviViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private data class CounterState(val count: Int = 0) : UiState

    private sealed interface CounterIntent : UiIntent {
        data object Increment : CounterIntent
        data object Celebrate : CounterIntent
    }

    private sealed interface CounterEffect : UiEffect {
        data object Confetti : CounterEffect
    }

    private class CounterViewModel : MviViewModel<CounterState, CounterIntent, CounterEffect>(CounterState()) {
        override suspend fun handle(intent: CounterIntent) {
            when (intent) {
                CounterIntent.Increment -> reduce { copy(count = count + 1) }
                CounterIntent.Celebrate -> emit(CounterEffect.Confetti)
            }
        }
    }

    @Test
    fun initialStateIsExposedBeforeAnyIntent() {
        val viewModel = CounterViewModel()

        assertEquals(CounterState(count = 0), viewModel.state.value)
    }

    @Test
    fun dispatchingAnIntentReducesState() = runTest {
        val viewModel = CounterViewModel()

        viewModel.dispatch(CounterIntent.Increment)
        viewModel.dispatch(CounterIntent.Increment)

        assertEquals(2, viewModel.state.value.count)
    }

    @Test
    fun effectsAreDeliveredExactlyOnceToTheCollector() = runTest {
        val viewModel = CounterViewModel()

        viewModel.effects.test {
            viewModel.dispatch(CounterIntent.Celebrate)
            assertEquals(CounterEffect.Confetti, awaitItem())
            expectNoEvents()
        }
    }
}
