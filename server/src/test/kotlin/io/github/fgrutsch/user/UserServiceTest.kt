package io.github.fgrutsch.user

import io.github.fgrutsch.shopping.ShoppingListRepository
import io.github.fgrutsch.support.BaseTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.koin.test.inject
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserServiceTest : BaseTest() {

    private val service: UserService by inject()

    @Test
    fun `getOrCreate creates a new user`() = runTest {
        val user = service.getOrCreate("oidc-subject-1")

        assertEquals("oidc-subject-1", user.oidcSubject)
    }

    @Test
    fun `getOrCreate is idempotent`() = runTest {
        val first = service.getOrCreate("oidc-subject-1")
        val second = service.getOrCreate("oidc-subject-1")

        assertEquals(first, second)
    }

    @Test
    fun `getOrCreate creates default shopping list for new user`() = runTest {
        val shoppingListRepo by inject<ShoppingListRepository>()

        val user = service.getOrCreate("oidc-subject-1")
        val lists = shoppingListRepo.findByUserId(user.id)

        assertEquals(1, lists.size)
        assertEquals("Shopping List", lists.first().name)
        assertTrue(lists.first().default)
    }

    @Test
    fun `findIdByOidcSubject returns id for existing user`() = runTest {
        val user = service.getOrCreate("oidc-subject-1")

        val id = service.findIdByOidcSubject("oidc-subject-1")

        assertEquals(user.id, id)
    }

    @Test
    fun `findIdByOidcSubject returns null for unknown subject`() = runTest {
        assertNull(service.findIdByOidcSubject("unknown"))
    }
}
