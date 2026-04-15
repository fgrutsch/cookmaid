package io.github.fgrutsch.cookmaid.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
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

    /**
     * Applies [reducer] to the current state and emits the result.
     *
     * @param reducer function applied to the current state to produce the new state.
     */
    protected fun updateState(reducer: S.() -> S) {
        state.update(reducer)
    }

    /**
     * Sends a one-shot [effect] to the UI layer.
     *
     * @param effect the side-effect to deliver to the UI (e.g. navigation, snackbar).
     */
    protected fun sendEffect(effect: F) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }

    /**
     * Launches a coroutine in viewModelScope, routing exceptions to [onError].
     *
     * @param block the suspend function to execute.
     */
    protected fun launch(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception
            ) {
                onError(e)
            }
        }
    }

    /**
     * Applies an optimistic state change, then runs a suspend block.
     * If the block throws, the state is rolled back to the snapshot taken before the change.
     *
     * Known race: the snapshot is restored unconditionally on failure, so an
     * in-flight optimistic operation that completes with an exception *after*
     * a logout-triggered `resetState()` will restore the pre-logout snapshot
     * into the just-cleared state. Fix would require a generation counter or
     * cancelling `viewModelScope` children on logout; out of scope for #73.
     *
     * @param optimisticUpdate reducer applied immediately before the async operation.
     * @param block the suspend function to execute after the optimistic update.
     */
    protected fun launchOptimistic(optimisticUpdate: S.() -> S, block: suspend () -> Unit) {
        val snapshot = state.value
        updateState(optimisticUpdate)
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception
            ) {
                state.update { snapshot }
                onError(e)
            }
        }
    }

    /**
     * Called when [launch] or [launchOptimistic] catches a non-cancellation exception.
     *
     * @param e the exception that was caught.
     */
    protected open fun onError(e: Exception) {}
}
