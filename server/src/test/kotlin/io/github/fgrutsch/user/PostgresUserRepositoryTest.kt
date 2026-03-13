package io.github.fgrutsch.user

import io.github.fgrutsch.support.BaseTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.koin.test.inject
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PostgresUserRepositoryTest : BaseTest() {

    private val repository: UserRepository by inject()

    @Test
    fun `getOrCreate creates a new user`() = runTest {
        val user = repository.getOrCreate("oidc-subject-1")

        assertEquals("oidc-subject-1", user.oidcSubject)
    }

    @Test
    fun `getOrCreate returns existing user on second call`() = runTest {
        val first = repository.getOrCreate("oidc-subject-1")
        val second = repository.getOrCreate("oidc-subject-1")

        assertEquals(first.id, second.id)
        assertEquals(first.oidcSubject, second.oidcSubject)
    }

    @Test
    fun `getOrCreate creates different users for different subjects`() = runTest {
        val user1 = repository.getOrCreate("oidc-subject-1")
        val user2 = repository.getOrCreate("oidc-subject-2")

        assertNotEquals(user1.id, user2.id)
        assertEquals("oidc-subject-1", user1.oidcSubject)
        assertEquals("oidc-subject-2", user2.oidcSubject)
    }
}
