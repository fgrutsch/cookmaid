package io.github.fgrutsch.cookmaid.ui.user

import io.github.fgrutsch.cookmaid.user.User
import kotlin.uuid.Uuid

class FakeUserClient : UserClient {
    var deleteCalled: Boolean = false
    var failDelete: Boolean = false

    override suspend fun getOrCreateUser(): User =
        User(id = Uuid.random(), oidcSubject = "test-subject")

    override suspend fun deleteAccount() {
        if (failDelete) throw IllegalStateException("delete failed")
        deleteCalled = true
    }
}
