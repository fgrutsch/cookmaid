package io.github.fgrutsch.cookmaid.user

import io.github.fgrutsch.cookmaid.shopping.ShoppingListRepository
import io.github.fgrutsch.cookmaid.support.BaseTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.koin.test.inject
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

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
        val lists = shoppingListRepo.find(UserId(user.id))

        assertEquals(1, lists.size)
        assertEquals("Shopping List", lists.first().name)
        assertTrue(lists.first().default)
    }

    @Test
    fun `findIdByOidcSubject returns id for existing user`() = runTest {
        val user = service.getOrCreate("oidc-subject-1")

        val id = service.findIdByOidcSubject("oidc-subject-1")

        assertEquals(user.id, id?.value)
    }

    @Test
    fun `findIdByOidcSubject returns null for unknown subject`() = runTest {
        assertNull(service.findIdByOidcSubject("unknown"))
    }

    @Test
    fun `delete removes the user and evicts the cache`() = runTest {
        val user = service.getOrCreate("oidc-subject-1")
        service.findIdByOidcSubject("oidc-subject-1") // prime the cache

        service.delete(UserId(user.id), "oidc-subject-1")

        assertNull(service.findIdByOidcSubject("oidc-subject-1"))
    }

    @Test
    fun `delete cascades to the user's shopping lists`() = runTest {
        val shoppingListRepo by inject<ShoppingListRepository>()
        val user = service.getOrCreate("oidc-subject-1")
        assertEquals(1, shoppingListRepo.find(UserId(user.id)).size)

        service.delete(UserId(user.id), "oidc-subject-1")

        assertEquals(0, shoppingListRepo.find(UserId(user.id)).size)
    }

    @Test
    fun `delete does not affect other users`() = runTest {
        val user1 = service.getOrCreate("oidc-subject-1")
        val user2 = service.getOrCreate("oidc-subject-2")

        service.delete(UserId(user1.id), "oidc-subject-1")

        assertNull(service.findIdByOidcSubject("oidc-subject-1"))
        assertEquals(user2.id, service.findIdByOidcSubject("oidc-subject-2")?.value)
    }
}
