package io.github.fgrutsch.cookmaid.ui.auth

import io.github.fgrutsch.cookmaid.user.User

interface AuthHandler {
    suspend fun tryAutoLogin(): AuthResult
    suspend fun login(): AuthResult
    suspend fun logout()
}

data class AuthResult(
    val user: User,
    val profile: UserProfile,
)
