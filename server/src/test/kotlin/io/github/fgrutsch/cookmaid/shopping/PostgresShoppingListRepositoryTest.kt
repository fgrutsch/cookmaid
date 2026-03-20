package io.github.fgrutsch.cookmaid.shopping

import io.github.fgrutsch.cookmaid.catalog.CatalogItemRepository
import io.github.fgrutsch.cookmaid.catalog.Item
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

class PostgresShoppingListRepositoryTest : BaseTest() {

    private suspend fun createUser(subject: String = "test-subject"): Uuid {
        val userRepo = getKoin().get<UserRepository>()
        return userRepo.create(subject).id
    }

    @Test
    fun `createList creates a new list`() = runTest {
        val repo = getKoin().get<ShoppingListRepository>()
        val userId = createUser()

        val list = repo.createList(userId, "Groceries")

        assertNotNull(list.id)
        assertEquals("Groceries", list.name)
        assertEquals(false, list.default)
    }

    @Test
    fun `createList creates a default list`() = runTest {
        val repo = getKoin().get<ShoppingListRepository>()
        val userId = createUser()

        val list = repo.createList(userId, "Groceries", default = true)

        assertTrue(list.default)
    }

    @Test
    fun `findByUserId returns lists for user`() = runTest {
        val repo = getKoin().get<ShoppingListRepository>()
        val userId = createUser()
        repo.createList(userId, "Groceries")
        repo.createList(userId, "BBQ Party")

        val lists = repo.findByUserId(userId)

        assertEquals(2, lists.size)
    }

    @Test
    fun `findByUserId returns empty for user with no lists`() = runTest {
        val repo = getKoin().get<ShoppingListRepository>()
        val userId = createUser()

        val lists = repo.findByUserId(userId)

        assertTrue(lists.isEmpty())
    }

    @Test
    fun `updateList updates name`() = runTest {
        val repo = getKoin().get<ShoppingListRepository>()
        val userId = createUser()
        val list = repo.createList(userId, "Old Name")

        repo.updateList(list.id, "New Name")

        val updated = repo.findByUserId(userId).first()
        assertEquals("New Name", updated.name)
    }

    @Test
    fun `deleteList removes the list and its items`() = runTest {
        val repo = getKoin().get<ShoppingListRepository>()
        val userId = createUser()
        val list = repo.createList(userId, "To Delete")
        repo.addItem(list.id, catalogItemId = null, freeTextName = "Test", quantity = null)

        repo.deleteList(list.id)

        assertTrue(repo.findByUserId(userId).isEmpty())
    }

    @Test
    fun `addItem adds a free text item`() = runTest {
        val repo = getKoin().get<ShoppingListRepository>()
        val userId = createUser()
        val list = repo.createList(userId, "Groceries")

        val created = repo.addItem(list.id, catalogItemId = null, freeTextName = "Paper towels", quantity = 2f)

        assertEquals("Paper towels", created.item.name)
        assertEquals(2f, created.quantity)
        assertTrue(created.item is Item.FreeText)
    }

    @Test
    fun `addItem adds a catalog item`() = runTest {
        val repo = getKoin().get<ShoppingListRepository>()
        val catalogRepo = getKoin().get<CatalogItemRepository>()
        val userId = createUser()
        val list = repo.createList(userId, "Groceries")
        val catalogItem = catalogRepo.findAll().first()

        val created = repo.addItem(list.id, catalogItemId = catalogItem.id, freeTextName = null, quantity = 3f)

        val addedItem = created.item as Item.Catalog
        assertEquals(catalogItem.name, addedItem.name)
        assertEquals(catalogItem.category.name, addedItem.category.name)
        assertEquals(catalogItem.category.id, addedItem.category.id)
    }

    @Test
    fun `updateItem updates quantity and checked`() = runTest {
        val repo = getKoin().get<ShoppingListRepository>()
        val userId = createUser()
        val list = repo.createList(userId, "Groceries")
        val item = repo.addItem(list.id, catalogItemId = null, freeTextName = "Eggs", quantity = 6f)

        repo.updateItem(item.id, quantity = 12f, checked = true)

        val items = repo.findItemsByListId(list.id)
        assertEquals(12f, items.first().quantity)
        assertTrue(items.first().checked)
    }

    @Test
    fun `deleteCheckedItems removes only checked items`() = runTest {
        val repo = getKoin().get<ShoppingListRepository>()
        val userId = createUser()
        val list = repo.createList(userId, "Groceries")
        repo.addItem(list.id, catalogItemId = null, freeTextName = "Keep", quantity = null)
        val item2 = repo.addItem(list.id, catalogItemId = null, freeTextName = "Remove", quantity = null)
        repo.updateItem(item2.id, quantity = null, checked = true)

        repo.deleteCheckedItems(list.id)

        val items = repo.findItemsByListId(list.id)
        assertEquals(1, items.size)
        assertEquals("Keep", items.first().item.name)
    }

    @Test
    fun `deleteItem removes a single item`() = runTest {
        val repo = getKoin().get<ShoppingListRepository>()
        val userId = createUser()
        val list = repo.createList(userId, "Groceries")
        val item = repo.addItem(list.id, catalogItemId = null, freeTextName = "Remove me", quantity = null)

        repo.deleteItem(item.id)

        val items = repo.findItemsByListId(list.id)
        assertTrue(items.isEmpty())
    }

    @Test
    fun `createList trims whitespace from name`() = runTest {
        val repo = getKoin().get<ShoppingListRepository>()
        val userId = createUser()

        val list = repo.createList(userId, "  Groceries  ")

        assertEquals("Groceries", list.name)
    }

    @Test
    fun `updateList trims whitespace from name`() = runTest {
        val repo = getKoin().get<ShoppingListRepository>()
        val userId = createUser()
        val list = repo.createList(userId, "Old Name")

        repo.updateList(list.id, "  New Name  ")

        val updated = repo.findByUserId(userId).first()
        assertEquals("New Name", updated.name)
    }

    @Test
    fun `findById returns list`() = runTest {
        val repo = getKoin().get<ShoppingListRepository>()
        val userId = createUser()
        val list = repo.createList(userId, "My List", default = true)

        val found = repo.findById(list.id)

        assertNotNull(found)
        assertEquals(list.id, found.id)
        assertEquals("My List", found.name)
        assertTrue(found.default)
    }

    @Test
    fun `findById returns null for nonexistent id`() = runTest {
        val repo = getKoin().get<ShoppingListRepository>()

        assertNull(repo.findById(Uuid.random()))
    }

    @Test
    fun `isListOwnedByUser returns true for own list`() = runTest {
        val repo = getKoin().get<ShoppingListRepository>()
        val userId = createUser()
        val list = repo.createList(userId, "My List")

        assertTrue(repo.isListOwnedByUser(userId, list.id))
    }

    @Test
    fun `isListOwnedByUser returns false for another users list`() = runTest {
        val repo = getKoin().get<ShoppingListRepository>()
        val userId = createUser("user-1")
        val otherUserId = createUser("user-2")
        val list = repo.createList(userId, "My List")

        assertFalse(repo.isListOwnedByUser(otherUserId, list.id))
    }

    @Test
    fun `isItemOwnedByUser returns true for own item`() = runTest {
        val repo = getKoin().get<ShoppingListRepository>()
        val userId = createUser()
        val list = repo.createList(userId, "Groceries")
        val item = repo.addItem(list.id, catalogItemId = null, freeTextName = "My item", quantity = null)

        assertTrue(repo.isItemOwnedByUser(userId, item.id))
    }

    @Test
    fun `isItemOwnedByUser returns false for another users item`() = runTest {
        val repo = getKoin().get<ShoppingListRepository>()
        val userId = createUser("user-1")
        val otherUserId = createUser("user-2")
        val list = repo.createList(userId, "Groceries")
        val item = repo.addItem(list.id, catalogItemId = null, freeTextName = "My item", quantity = null)

        assertFalse(repo.isItemOwnedByUser(otherUserId, item.id))
    }
}
