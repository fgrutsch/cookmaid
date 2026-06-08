package io.github.fgrutsch.cookmaid.ui.deleteaccount

/**
 * @param deleting true while the delete request is in flight.
 * @param deleted true once the account has been deleted.
 * @param error true if the delete request failed.
 */
data class DeleteAccountState(
    val deleting: Boolean = false,
    val deleted: Boolean = false,
    val error: Boolean = false,
)

sealed interface DeleteAccountEvent {
    data object Confirm : DeleteAccountEvent
}
