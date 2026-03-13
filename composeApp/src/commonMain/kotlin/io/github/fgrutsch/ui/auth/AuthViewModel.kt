package io.github.fgrutsch.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fgrutsch.auth.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: OidcAuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Initializing)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                authRepository.tryAutoLogin()
                _state.value = authenticated()
            } catch (_: Exception) {
                _state.value = AuthState.Unauthenticated()
            }
        }
    }

    fun login() {
        viewModelScope.launch {
            _state.value = AuthState.Initializing
            try {
                authRepository.login()
                _state.value = authenticated()
            } catch (e: Exception) {
                _state.value = AuthState.Unauthenticated(loginError = e.message ?: "Login failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _state.value = AuthState.Unauthenticated()
        }
    }

    private fun authenticated() = AuthState.Authenticated(
        user = authRepository.currentUser.value!!,
        profile = authRepository.userProfile,
    )
}

sealed interface AuthState {
    data object Initializing : AuthState
    data class Unauthenticated(val loginError: String? = null) : AuthState
    data class Authenticated(val user: User, val profile: UserProfile) : AuthState
}
