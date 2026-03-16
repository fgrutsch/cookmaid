package io.github.fgrutsch.cookmaid.shopping

import io.github.fgrutsch.cookmaid.support.BaseTest
import io.github.fgrutsch.cookmaid.user.UserRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class ShoppingListServiceTest : BaseTest() {

    private suspend fun createUser(subject: String = "test-subject"): Uuid {
        val userRepo = getKoin().get<UserRepository>()
        return userRepo.create(subject).id
    }

    private suspend fun createUserWithList(subject: String = "test-subject"): Pair<Uuid, ShoppingList> {
        val userId = createUser(subject)
        val repo = getKoin().get<ShoppingListRepository>()
        val list = repo.createList(userId, "Default", default = true)
        return userId to list
    }

    @Test
    fun `findListsByUser returns lists`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val (userId, _) = createUserWithList()

        val lists = service.findListsByUser(userId)

        assertEquals(1, lists.size)
    }

    @Test
    fun `createList always creates non-default list`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val userId = createUser()

        val list = service.createList(userId, "Groceries")

        assertFalse(list.default)
    }

    @Test
    fun `updateList succeeds for own list`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val (userId, list) = createUserWithList()

        val result = service.updateList(userId, list.id, "New Name")

        assertTrue(result)
    }

    @Test
    fun `updateList fails for another users list`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val (_, list) = createUserWithList("user-1")
        val otherUserId = createUser("user-2")

        val result = service.updateList(otherUserId, list.id, "Hacked")

        assertFalse(result)
    }

    @Test
    fun `deleteList succeeds for non-default list`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val userId = createUser()
        val list = service.createList(userId, "Temp")

        val result = service.deleteList(userId, list.id)

        assertEquals(ShoppingListService.DeleteListResult.Deleted, result)
    }

    @Test
    fun `deleteList returns CannotDeleteDefault for default list`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val (userId, defaultList) = createUserWithList()

        val result = service.deleteList(userId, defaultList.id)

        assertEquals(ShoppingListService.DeleteListResult.CannotDeleteDefault, result)
    }

    @Test
    fun `deleteList returns NotFound for another users list`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val (_, list) = createUserWithList("user-1")
        val otherUserId = createUser("user-2")

        val result = service.deleteList(otherUserId, list.id)

        assertEquals(ShoppingListService.DeleteListResult.NotFound, result)
    }

    @Test
    fun `deleteList returns NotFound for nonexistent list`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val userId = createUser()

        val result = service.deleteList(userId, Uuid.random())

        assertEquals(ShoppingListService.DeleteListResult.NotFound, result)
    }

    @Test
    fun `findItemsByListId returns items for own list`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val repo = getKoin().get<ShoppingListRepository>()
        val (userId, list) = createUserWithList()
        repo.addItem(list.id, catalogItemId = null, freeTextName = "Eggs", quantity = null)

        val items = service.findItemsByListId(userId, list.id)

        assertEquals(1, items.size)
    }

    @Test
    fun `findItemsByListId returns empty for another users list`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val repo = getKoin().get<ShoppingListRepository>()
        val (userId, list) = createUserWithList("user-1")
        repo.addItem(list.id, catalogItemId = null, freeTextName = "Eggs", quantity = null)
        val otherUserId = createUser("user-2")

        val items = service.findItemsByListId(otherUserId, list.id)

        assertTrue(items.isEmpty())
    }

    @Test
    fun `addItem succeeds for own list`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val (userId, list) = createUserWithList()

        val item = service.addItem(userId, list.id, null, "Milk", 1f)

        assertNotNull(item)
        assertEquals("Milk", item.item.name)
    }

    @Test
    fun `addItem returns null for another users list`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val (_, list) = createUserWithList("user-1")
        val otherUserId = createUser("user-2")

        val item = service.addItem(otherUserId, list.id, null, "Hacked", null)

        assertNull(item)
    }

    @Test
    fun `updateItem succeeds for own item`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val repo = getKoin().get<ShoppingListRepository>()
        val (userId, list) = createUserWithList()
        val item = repo.addItem(list.id, catalogItemId = null, freeTextName = "Eggs", quantity = 6f)

        val result = service.updateItem(userId, item.id, quantity = 12f, checked = true)

        assertTrue(result)
    }

    @Test
    fun `updateItem fails for another users item`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val repo = getKoin().get<ShoppingListRepository>()
        val (_, list) = createUserWithList("user-1")
        val item = repo.addItem(list.id, catalogItemId = null, freeTextName = "Eggs", quantity = 6f)
        val otherUserId = createUser("user-2")

        val result = service.updateItem(otherUserId, item.id, quantity = 99f, checked = true)

        assertFalse(result)
    }

    @Test
    fun `deleteItem succeeds for own item`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val repo = getKoin().get<ShoppingListRepository>()
        val (userId, list) = createUserWithList()
        val item = repo.addItem(list.id, catalogItemId = null, freeTextName = "Remove", quantity = null)

        val result = service.deleteItem(userId, item.id)

        assertTrue(result)
    }

    @Test
    fun `deleteItem fails for another users item`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val repo = getKoin().get<ShoppingListRepository>()
        val (_, list) = createUserWithList("user-1")
        val item = repo.addItem(list.id, catalogItemId = null, freeTextName = "Item", quantity = null)
        val otherUserId = createUser("user-2")

        val result = service.deleteItem(otherUserId, item.id)

        assertFalse(result)
    }

    @Test
    fun `deleteCheckedItems succeeds for own list`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val repo = getKoin().get<ShoppingListRepository>()
        val (userId, list) = createUserWithList()
        val item = repo.addItem(list.id, catalogItemId = null, freeTextName = "Done", quantity = null)
        repo.updateItem(item.id, quantity = null, checked = true)

        val result = service.deleteCheckedItems(userId, list.id)

        assertTrue(result)
        assertTrue(repo.findItemsByListId(list.id).isEmpty())
    }

    @Test
    fun `deleteCheckedItems fails for another users list`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val (_, list) = createUserWithList("user-1")
        val otherUserId = createUser("user-2")

        val result = service.deleteCheckedItems(otherUserId, list.id)

        assertFalse(result)
    }
}
