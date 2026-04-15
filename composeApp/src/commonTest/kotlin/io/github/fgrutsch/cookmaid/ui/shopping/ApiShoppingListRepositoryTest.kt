package io.github.fgrutsch.cookmaid.ui.shopping

import io.github.fgrutsch.cookmaid.shopping.CreateShoppingItemRequest
import io.github.fgrutsch.cookmaid.shopping.ShoppingItem
import io.github.fgrutsch.cookmaid.shopping.ShoppingList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class ApiShoppingListRepositoryTest {

    private val fakeClient = FakeShoppingListClient()
    private val repository = ApiShoppingListRepository(fakeClient)

    @Test
    fun `getLists caches the first fetch`() = runTest {
        fakeClient.listsToReturn = listOf(ShoppingList(id = Uuid.random(), name = "A"))

        repository.getLists()
        repository.getLists()

        assertEquals(1, fakeClient.fetchListsCallCount)
    }

    @Test
    fun `clear drops cache so next call refetches from client`() = runTest {
        fakeClient.listsToReturn = listOf(ShoppingList(id = Uuid.random(), name = "A"))
        repository.getLists()
        assertEquals(1, fakeClient.fetchListsCallCount)

        repository.clear()
        fakeClient.listsToReturn = listOf(ShoppingList(id = Uuid.random(), name = "B"))
        val refreshed = repository.getLists()

        assertEquals(2, fakeClient.fetchListsCallCount)
        assertEquals("B", refreshed.single().name)
    }

    @Test
    fun `clear on empty cache is a no-op and does not call the client`() = runTest {
        repository.clear()

        assertEquals(0, fakeClient.fetchListsCallCount)
    }
}

private class FakeShoppingListClient : ShoppingListClient {

    var listsToReturn: List<ShoppingList> = emptyList()
    var fetchListsCallCount: Int = 0

    override suspend fun fetchLists(): List<ShoppingList> {
        fetchListsCallCount++
        return listsToReturn
    }

    override suspend fun createList(name: String): ShoppingList = error("not used")
    override suspend fun updateList(id: Uuid, name: String) = error("not used")
    override suspend fun deleteList(id: Uuid) = error("not used")
    override suspend fun fetchItems(listId: Uuid): List<ShoppingItem> = error("not used")
    override suspend fun addItems(listId: Uuid, items: List<CreateShoppingItemRequest>): List<ShoppingItem> =
        error("not used")

    override suspend fun addItem(
        listId: Uuid,
        catalogItemId: Uuid?,
        freeTextName: String?,
        quantity: String?,
    ): ShoppingItem = error("not used")

    override suspend fun updateItem(listId: Uuid, itemId: Uuid, quantity: String?, checked: Boolean) =
        error("not used")

    override suspend fun deleteItem(listId: Uuid, itemId: Uuid) = error("not used")
    override suspend fun deleteCheckedItems(listId: Uuid) = error("not used")
}
