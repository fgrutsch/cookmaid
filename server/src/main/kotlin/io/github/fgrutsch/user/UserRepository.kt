package io.github.fgrutsch.user

import io.github.fgrutsch.auth.User
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

interface UserRepository {
    suspend fun findByOidcSubject(oidcSubject: String): User?
    suspend fun findOrCreate(oidcSubject: String): User
}

class InMemoryUserRepository : UserRepository {
    private val users = ConcurrentHashMap<String, User>()

    override suspend fun findByOidcSubject(oidcSubject: String): User? =
        users.values.find { it.oidcSubject == oidcSubject }

    override suspend fun findOrCreate(oidcSubject: String): User =
        findByOidcSubject(oidcSubject) ?: run {
            val user = User(
                id = UUID.randomUUID().toString(),
                oidcSubject = oidcSubject,
            )
            users[user.id] = user

            user
        }
}
