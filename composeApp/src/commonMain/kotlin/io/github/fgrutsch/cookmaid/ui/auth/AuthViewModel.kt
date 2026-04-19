package io.github.fgrutsch.cookmaid.ui.auth

import io.github.fgrutsch.cookmaid.ui.common.MviViewModel
import kotlinx.coroutines.CancellationException

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
        // Flip identity to Unauthenticated synchronously so the Koin session
        // reset keyed on `user.id` in App.kt fires immediately — before any
        // observer sees `status == Authenticated && user == null`.
        updateState {
            copy(
                status = AuthState.Status.Unauthenticated,
                user = null,
                profile = UserProfile(),
                loginError = null,
            )
        }
        launch {
            try {
                authHandler.logout()
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception
            ) {
                // Token/transport cleanup failed. State is already Unauthenticated
                // so the UI is safe, but stale tokens may persist on disk and
                // auto-login as the previous user on next app start. Log loudly
                // so the failure is diagnosable instead of silently swallowed by
                // MviViewModel.onError.
                e.printStackTrace()
            }
        }
    }
}
