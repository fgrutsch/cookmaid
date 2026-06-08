package io.github.fgrutsch.cookmaid.ui.auth

import io.github.fgrutsch.cookmaid.user.User

data class AuthState(
    val status: Status = Status.Initializing,
    val user: User? = null,
    val profile: UserProfile = UserProfile(),
    val loginError: String? = null,
    val accountDeleted: Boolean = false,
) {
    enum class Status { Initializing, Unauthenticated, Authenticated }
}

sealed interface AuthEvent {
    data object Initialize : AuthEvent
    data object Login : AuthEvent
    data object Logout : AuthEvent

    /** Logs out after the user's account was deleted, flagging a one-shot confirmation message. */
    data object AccountDeleted : AuthEvent

    /** Clears the one-shot account-deleted message after it has been shown. */
    data object AccountDeletedMessageShown : AuthEvent
}

sealed interface AuthEffect
