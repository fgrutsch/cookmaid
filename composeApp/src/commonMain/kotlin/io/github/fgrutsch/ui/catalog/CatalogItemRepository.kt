package io.github.fgrutsch.ui.catalog

import io.github.fgrutsch.catalog.Item
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface CatalogItemRepository {
    suspend fun search(query: String): List<Item.CatalogItem>
}

class ApiCatalogItemRepository(
    private val client: CatalogItemClient,
) : CatalogItemRepository {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var cachedItems: List<Item.CatalogItem> = emptyList()

    init {
        scope.launch { loadItems() }
    }

    override suspend fun search(query: String): List<Item.CatalogItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        if (cachedItems.isEmpty()) loadItems()
        return cachedItems
            .filter { it.name.lowercase().contains(q) }
            .take(5)
    }

    private suspend fun loadItems() {
        cachedItems = client.fetchAll()
    }
}
