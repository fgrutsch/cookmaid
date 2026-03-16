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
            } catch (e: Exception) {
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
        launch {
            authHandler.logout()
            updateState {
                copy(
                    status = AuthState.Status.Unauthenticated,
                    user = null,
                    profile = UserProfile(),
                    loginError = null,
                )
            }
        }
    }
}
