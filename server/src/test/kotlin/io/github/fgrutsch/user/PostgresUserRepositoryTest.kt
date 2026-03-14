package io.github.fgrutsch.user

import io.github.fgrutsch.support.BaseTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.koin.test.inject
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PostgresUserRepositoryTest : BaseTest() {

    private val repository: UserRepository by inject()

    @Test
    fun `create creates a new user`() = runTest {
        val user = repository.create("oidc-subject-1")

        assertEquals("oidc-subject-1", user.oidcSubject)
        assertNotNull(user.id)
    }

    @Test
    fun `findByOidcSubject returns existing user`() = runTest {
        val created = repository.create("oidc-subject-1")
        val found = repository.findByOidcSubject("oidc-subject-1")

        assertNotNull(found)
        assertEquals(created.id, found.id)
        assertEquals(created.oidcSubject, found.oidcSubject)
    }

    @Test
    fun `findByOidcSubject returns null for unknown subject`() = runTest {
        assertNull(repository.findByOidcSubject("unknown"))
    }

    @Test
    fun `create creates different users for different subjects`() = runTest {
        val user1 = repository.create("oidc-subject-1")
        val user2 = repository.create("oidc-subject-2")

        assertNotEquals(user1.id, user2.id)
        assertEquals("oidc-subject-1", user1.oidcSubject)
        assertEquals("oidc-subject-2", user2.oidcSubject)
    }
}
