package io.github.fgrutsch.user

import io.github.fgrutsch.auth.User

interface UserRepository {
    suspend fun getOrCreate(oidcSubject: String): User
}
