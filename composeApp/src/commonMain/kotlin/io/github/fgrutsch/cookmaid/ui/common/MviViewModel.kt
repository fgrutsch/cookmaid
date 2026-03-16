package io.github.fgrutsch.cookmaid.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base ViewModel implementing the MVI (Model-View-Intent) pattern.
 *
 * @param S The state type representing the UI state.
 * @param E The event type representing user actions.
 * @param F The effect type representing one-shot side effects (navigation, snackbars, etc.).
 */
abstract class MviViewModel<S, E, F>(initialState: S) : ViewModel() {

    val state: StateFlow<S>
        field = MutableStateFlow(initialState)

    private val _effects = Channel<F>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: E) {
        handleEvent(event)
    }

    protected abstract fun handleEvent(event: E)

    protected fun updateState(reducer: S.() -> S) {
        state.update(reducer)
    }

    protected fun sendEffect(effect: F) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }

    protected fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
