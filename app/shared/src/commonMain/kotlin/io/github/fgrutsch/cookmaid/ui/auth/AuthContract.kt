package io.github.fgrutsch.cookmaid.ui.auth

import io.github.fgrutsch.cookmaid.user.User

data class AuthState(
    val status: Status = Status.Initializing,
    val user: User? = null,
    val profile: UserProfile = UserProfile(),
    val loginError: String? = null,
) {
    enum class Status { Initializing, Unauthenticated, Authenticated }
}

sealed interface AuthEvent {
    data object Initialize : AuthEvent
    data object Login : AuthEvent
    data object Logout : AuthEvent

    /** Logs out after the user's account was deleted, emitting [AuthEffect.AccountDeleted]. */
    data object AccountDeleted : AuthEvent
}

sealed interface AuthEffect {
    /** The account was deleted; the login screen shows a one-shot confirmation. */
    data object AccountDeleted : AuthEffect
}
