package io.github.fgrutsch.cookmaid.ui.auth

import io.github.fgrutsch.cookmaid.user.User
import kotlin.uuid.Uuid

class FakeAuthHandler : AuthHandler {

    var resultToReturn: AuthResult = AuthResult(
        User(id = Uuid.random(), oidcSubject = "default"),
        UserProfile(),
    )
    var shouldFail: Boolean = false
    var failMessage: String = "Auth failed"

    override suspend fun tryAutoLogin(): AuthResult {
        if (shouldFail) throw IllegalStateException(failMessage)
        return resultToReturn
    }

    override suspend fun login(): AuthResult {
        if (shouldFail) throw IllegalStateException(failMessage)
        return resultToReturn
    }

    override suspend fun logout() = Unit
}
