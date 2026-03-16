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
}

sealed interface AuthEffect
