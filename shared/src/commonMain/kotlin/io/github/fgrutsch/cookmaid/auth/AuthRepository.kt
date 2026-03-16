package io.github.fgrutsch.cookmaid.auth

import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<User?>
    val isAuthenticated: StateFlow<Boolean>

    suspend fun login()
    suspend fun logout()
}
