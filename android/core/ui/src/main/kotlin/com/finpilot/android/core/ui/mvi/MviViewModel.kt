package com.finpilot.android.core.ui.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Marker for an immutable, renderable screen state. One data class per screen. */
interface UiState

/** Marker for user actions and lifecycle triggers a screen can dispatch. */
interface UiIntent

/** Marker for one-shot side effects (navigation, snackbars) that must not be replayed on recomposition. */
interface UiEffect

/**
 * MVI base: a single [state] stream reduced from intents, plus a buffered [effects] stream delivered
 * exactly once to the active collector. Subclasses implement [handle] and call [reduce] / [emit].
 */
abstract class MviViewModel<S : UiState, I : UiIntent, E : UiEffect>(
    initialState: S,
) : ViewModel() {

    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<S> = mutableState.asStateFlow()

    private val effectChannel = Channel<E>(Channel.BUFFERED)
    val effects: Flow<E> = effectChannel.receiveAsFlow()

    fun dispatch(intent: I) {
        viewModelScope.launch { handle(intent) }
    }

    protected abstract suspend fun handle(intent: I)

    protected fun reduce(block: S.() -> S) {
        mutableState.update { current -> current.block() }
    }

    protected suspend fun emit(effect: E) {
        effectChannel.send(effect)
    }
}
