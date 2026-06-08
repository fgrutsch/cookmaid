package io.github.fgrutsch.cookmaid.ui.auth

import io.github.fgrutsch.cookmaid.ui.common.MviViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException

class AuthViewModel(
    private val authHandler: AuthHandler,
) : MviViewModel<AuthState, AuthEvent, AuthEffect>(AuthState()) {

    private val logger = KotlinLogging.logger {}

    override fun handleEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.Initialize -> initialize()
            is AuthEvent.Login -> login()
            is AuthEvent.Logout -> clearSession(accountDeleted = false)
            is AuthEvent.AccountDeleted -> clearSession(accountDeleted = true)
            is AuthEvent.AccountDeletedMessageShown -> updateState { copy(accountDeleted = false) }
        }
    }

    private fun initialize() {
        launch {
            try {
                val result = authHandler.tryAutoLogin()
                logger.debug { "Auto-login succeeded" }
                updateState {
                    copy(
                        status = AuthState.Status.Authenticated,
                        user = result.user,
                        profile = result.profile,
                        loginError = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") _: Exception
            ) {
                logger.debug { "Auto-login failed, user is unauthenticated" }
                updateState { copy(status = AuthState.Status.Unauthenticated) }
            }
        }
    }

    private fun login() {
        updateState { copy(status = AuthState.Status.Initializing, loginError = null) }
        launch {
            try {
                val result = authHandler.login()
                logger.debug { "Login succeeded" }
                updateState {
                    copy(
                        status = AuthState.Status.Authenticated,
                        user = result.user,
                        profile = result.profile,
                        loginError = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
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

    /**
     * Clears the session: flips identity to Unauthenticated and clears tokens.
     *
     * @param accountDeleted when true, also raises the one-shot account-deleted
     *   confirmation flag for the login screen. Set after the user's account was
     *   deleted server-side — clearing tokens here prevents a reload from
     *   re-provisioning the user via auto-login while the IdP session is still valid.
     */
    private fun clearSession(accountDeleted: Boolean) {
        // Flip identity to Unauthenticated synchronously so the Koin session
        // reset keyed on `user.id` in App.kt fires immediately — before any
        // observer sees `status == Authenticated && user == null`.
        updateState {
            copy(
                status = AuthState.Status.Unauthenticated,
                user = null,
                profile = UserProfile(),
                loginError = null,
                accountDeleted = accountDeleted,
            )
        }
        logger.debug { if (accountDeleted) "Account deleted, clearing session" else "Logout initiated" }
        launch {
            try {
                authHandler.logout()
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception
            ) {
                logger.error(e) { "Logout cleanup failed" }
            }
        }
    }
}
