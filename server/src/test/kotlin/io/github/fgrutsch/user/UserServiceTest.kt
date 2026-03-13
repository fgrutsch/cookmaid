package io.github.fgrutsch.user

import io.github.fgrutsch.support.BaseTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.koin.test.inject
import kotlin.test.assertEquals

class UserServiceTest : BaseTest() {

    private val service: UserService by inject()

    @Test
    fun `getOrCreate returns user with correct oidcSubject`() = runTest {
        val user = service.getOrCreate("oidc-subject-1")

        assertEquals("oidc-subject-1", user.oidcSubject)
    }

    @Test
    fun `getOrCreate is idempotent`() = runTest {
        val first = service.getOrCreate("oidc-subject-1")
        val second = service.getOrCreate("oidc-subject-1")

        assertEquals(first, second)
    }
}
