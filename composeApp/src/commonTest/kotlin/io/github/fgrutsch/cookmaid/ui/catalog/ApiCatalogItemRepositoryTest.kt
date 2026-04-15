package io.github.fgrutsch.cookmaid.ui.catalog

import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.catalog.ItemCategory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class ApiCatalogItemRepositoryTest {

    private val fakeClient = FakeCatalogItemClient()
    private val repository = ApiCatalogItemRepository(fakeClient)

    @Test
    fun `search caches the first fetch`() = runTest {
        fakeClient.itemsToReturn = listOf(catalogItem("Milk"), catalogItem("Bread"))

        repository.search("milk")
        repository.search("bread")

        assertEquals(1, fakeClient.fetchAllCallCount)
    }

    @Test
    fun `clear drops cache so next search refetches from client`() = runTest {
        fakeClient.itemsToReturn = listOf(catalogItem("Milk"))
        repository.search("milk")
        assertEquals(1, fakeClient.fetchAllCallCount)

        repository.clear()
        fakeClient.itemsToReturn = listOf(catalogItem("Tomato"))
        val results = repository.search("tomato")

        assertEquals(2, fakeClient.fetchAllCallCount)
        assertEquals(1, results.size)
        assertEquals("Tomato", results.first().name)
    }

    @Test
    fun `clear on empty cache is a no-op and does not call the client`() = runTest {
        repository.clear()

        assertEquals(0, fakeClient.fetchAllCallCount)
    }

    @Test
    fun `blank search returns empty and does not populate cache`() = runTest {
        val results = repository.search("   ")

        assertTrue(results.isEmpty())
        assertEquals(0, fakeClient.fetchAllCallCount)
    }

    companion object {
        private val CATEGORY = ItemCategory(id = Uuid.random(), name = "General")
        private fun catalogItem(name: String) = Item.Catalog(id = Uuid.random(), name = name, category = CATEGORY)
    }
}

private class FakeCatalogItemClient : CatalogItemClient {

    var itemsToReturn: List<Item.Catalog> = emptyList()
    var fetchAllCallCount: Int = 0

    override suspend fun fetchAll(): List<Item.Catalog> {
        fetchAllCallCount++
        return itemsToReturn
    }
}
