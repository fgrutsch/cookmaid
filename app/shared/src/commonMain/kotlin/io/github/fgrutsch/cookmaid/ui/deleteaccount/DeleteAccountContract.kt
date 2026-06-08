package io.github.fgrutsch.cookmaid.ui.deleteaccount

/**
 * @param deleting true while the delete request is in flight (kept true through
 *   success until the screen is torn down by the ensuing logout).
 * @param error true if the delete request failed.
 */
data class DeleteAccountState(
    val deleting: Boolean = false,
    val error: Boolean = false,
)

sealed interface DeleteAccountEvent {
    data object Confirm : DeleteAccountEvent
}

sealed interface DeleteAccountEffect {
    /** The account was deleted server-side; the screen should hand off to logout. */
    data object Deleted : DeleteAccountEffect
}
