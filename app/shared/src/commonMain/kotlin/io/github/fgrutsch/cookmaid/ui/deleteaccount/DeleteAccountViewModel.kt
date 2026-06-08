package io.github.fgrutsch.cookmaid.ui.deleteaccount

import io.github.fgrutsch.cookmaid.ui.common.MviViewModel
import io.github.fgrutsch.cookmaid.ui.user.UserClient

/**
 * Drives the account deletion flow: calls the delete endpoint and tracks progress.
 *
 * @property userClient the client used to delete the account.
 */
class DeleteAccountViewModel(
    private val userClient: UserClient,
) : MviViewModel<DeleteAccountState, DeleteAccountEvent, DeleteAccountEffect>(DeleteAccountState()) {

    override fun handleEvent(event: DeleteAccountEvent) {
        when (event) {
            DeleteAccountEvent.Confirm -> confirm()
        }
    }

    private fun confirm() {
        if (state.value.deleting) return
        updateState { copy(deleting = true, error = false) }
        launch {
            userClient.deleteAccount()
            // Keep `deleting = true`: the screen hands off to logout on this
            // effect and is torn down, so the button must not flash back to active.
            sendEffect(DeleteAccountEffect.Deleted)
        }
    }

    override fun onError(e: Exception) {
        updateState { copy(deleting = false, error = true) }
    }
}
