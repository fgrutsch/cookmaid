package io.github.fgrutsch.cookmaid.shopping

import io.github.fgrutsch.cookmaid.common.SupportedLocale
import io.github.fgrutsch.cookmaid.support.BaseTest
import io.github.fgrutsch.cookmaid.user.UserId
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

    private suspend fun createUser(subject: String = "test-subject"): UserId {
        val userRepo = getKoin().get<UserRepository>()
        return UserId(userRepo.create(subject).id)
    }

    private suspend fun createUserWithList(subject: String = "test-subject"): Pair<UserId, ShoppingList> {
        val userId = createUser(subject)
        val repo = getKoin().get<ShoppingListRepository>()
        val list = repo.createList(userId, "Default", default = true)
        return userId to list
    }

    @Test
    fun `findLists returns lists`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val (userId, _) = createUserWithList()

        val lists = service.findLists(userId)

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

        assertEquals(DeleteListResult.Deleted, result)
    }

    @Test
    fun `deleteList returns CannotDeleteDefault for default list`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val (userId, defaultList) = createUserWithList()

        val result = service.deleteList(userId, defaultList.id)

        assertEquals(DeleteListResult.CannotDeleteDefault, result)
    }

    @Test
    fun `deleteList returns NotFound for another users list`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val (_, list) = createUserWithList("user-1")
        val otherUserId = createUser("user-2")

        val result = service.deleteList(otherUserId, list.id)

        assertEquals(DeleteListResult.NotFound, result)
    }

    @Test
    fun `deleteList returns NotFound for nonexistent list`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val userId = createUser()

        val result = service.deleteList(userId, Uuid.random())

        assertEquals(DeleteListResult.NotFound, result)
    }

    @Test
    fun `findItemsByListId returns items for own list`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val repo = getKoin().get<ShoppingListRepository>()
        val (userId, list) = createUserWithList()
        repo.addItem(list.id, catalogItemId = null, freeTextName = "Eggs", quantity = null, locale = SupportedLocale.EN)

        val items = service.findItemsByListId(userId, list.id, SupportedLocale.EN)

        assertNotNull(items)
        assertEquals(1, items.size)
    }

    @Test
    fun `findItemsByListId returns null for another users list`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val repo = getKoin().get<ShoppingListRepository>()
        val (_, list) = createUserWithList("user-1")
        repo.addItem(list.id, catalogItemId = null, freeTextName = "Eggs", quantity = null, locale = SupportedLocale.EN)
        val otherUserId = createUser("user-2")

        val items = service.findItemsByListId(otherUserId, list.id, SupportedLocale.EN)

        assertNull(items)
    }

    @Test
    fun `findItemsByListId returns empty list for owned list with no items`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val (userId, list) = createUserWithList()

        val items = service.findItemsByListId(userId, list.id, SupportedLocale.EN)

        assertNotNull(items)
        assertTrue(items.isEmpty())
    }

    @Test
    fun `addItem succeeds for own list`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val (userId, list) = createUserWithList()

        val item = service.addItem(userId, list.id, null, "Milk", "1", SupportedLocale.EN)

        assertNotNull(item)
        assertEquals("Milk", item.item.name)
    }

    @Test
    fun `addItem returns null for another users list`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val (_, list) = createUserWithList("user-1")
        val otherUserId = createUser("user-2")

        val item = service.addItem(otherUserId, list.id, null, "Hacked", null, SupportedLocale.EN)

        assertNull(item)
    }

    @Test
    fun `updateItem succeeds for own item`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val repo = getKoin().get<ShoppingListRepository>()
        val (userId, list) = createUserWithList()
        val item = repo.addItem(
            list.id, catalogItemId = null, freeTextName = "Eggs", quantity = "6", locale = SupportedLocale.EN,
        )

        val result = service.updateItem(userId, item.id, quantity = "12", checked = true)

        assertTrue(result)
    }

    @Test
    fun `updateItem fails for another users item`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val repo = getKoin().get<ShoppingListRepository>()
        val (_, list) = createUserWithList("user-1")
        val item = repo.addItem(
            list.id, catalogItemId = null, freeTextName = "Eggs", quantity = "6", locale = SupportedLocale.EN,
        )
        val otherUserId = createUser("user-2")

        val result = service.updateItem(otherUserId, item.id, quantity = "99", checked = true)

        assertFalse(result)
    }

    @Test
    fun `deleteItem succeeds for own item`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val repo = getKoin().get<ShoppingListRepository>()
        val (userId, list) = createUserWithList()
        val item = repo.addItem(
            list.id, catalogItemId = null, freeTextName = "Remove", quantity = null, locale = SupportedLocale.EN,
        )

        val result = service.deleteItem(userId, item.id)

        assertTrue(result)
    }

    @Test
    fun `deleteItem fails for another users item`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val repo = getKoin().get<ShoppingListRepository>()
        val (_, list) = createUserWithList("user-1")
        val item = repo.addItem(
            list.id, catalogItemId = null, freeTextName = "Item", quantity = null, locale = SupportedLocale.EN,
        )
        val otherUserId = createUser("user-2")

        val result = service.deleteItem(otherUserId, item.id)

        assertFalse(result)
    }

    @Test
    fun `deleteCheckedItems succeeds for own list`() = runTest {
        val service = getKoin().get<ShoppingListService>()
        val repo = getKoin().get<ShoppingListRepository>()
        val (userId, list) = createUserWithList()
        val item = repo.addItem(
            list.id, catalogItemId = null, freeTextName = "Done", quantity = null, locale = SupportedLocale.EN,
        )
        repo.updateItem(item.id, quantity = null, checked = true)

        val result = service.deleteCheckedItems(userId, list.id)

        assertTrue(result)
        assertTrue(repo.findItemsByListId(list.id, SupportedLocale.EN).isEmpty())
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
