package io.github.fgrutsch.cookmaid.ui.auth

import io.github.fgrutsch.cookmaid.ui.common.MviViewModel

class AuthViewModel(
    private val authHandler: AuthHandler,
) : MviViewModel<AuthState, AuthEvent, AuthEffect>(AuthState()) {

    override fun handleEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.Initialize -> initialize()
            is AuthEvent.Login -> login()
            is AuthEvent.Logout -> logout()
        }
    }

    private fun initialize() {
        launch {
            try {
                val result = authHandler.tryAutoLogin()
                updateState {
                    copy(
                        status = AuthState.Status.Authenticated,
                        user = result.user,
                        profile = result.profile,
                        loginError = null,
                    )
                }
            } catch (_: Exception) {
                updateState { copy(status = AuthState.Status.Unauthenticated) }
            }
        }
    }

    private fun login() {
        updateState { copy(status = AuthState.Status.Initializing, loginError = null) }
        launch {
            try {
                val result = authHandler.login()
                updateState {
                    copy(
                        status = AuthState.Status.Authenticated,
                        user = result.user,
                        profile = result.profile,
                        loginError = null,
                    )
                }
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception
            ) {
                updateState {
                    copy(
                        status = AuthState.Status.Unauthenticated,
                        loginError = e.message ?: "Login failed",
                    )
                }
            }
        }
    }

    private fun logout() {
        // Reset identity first, *before* the SessionCleaner runs. Otherwise
        // any composable observing AuthState during the cleanup window sees
        // the previous user's identity while the data layer is being erased.
        updateState { copy(user = null, profile = UserProfile()) }
        launch {
            authHandler.logout()
            updateState {
                copy(
                    status = AuthState.Status.Unauthenticated,
                    loginError = null,
                )
            }
        }
    }
}
