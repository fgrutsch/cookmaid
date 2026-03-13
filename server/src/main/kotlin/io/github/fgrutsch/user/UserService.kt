package io.github.fgrutsch.user

import io.github.fgrutsch.auth.User
import io.github.oshai.kotlinlogging.KotlinLogging

class UserService(private val repository: UserRepository) {

    private val logger = KotlinLogging.logger {}

    suspend fun getOrCreate(oidcSubject: String): User {
        val user = repository.getOrCreate(oidcSubject)
        logger.debug { "User resolved: id=${user.id}, oidcSubject=$oidcSubject" }
        return user
    }
}
