package io.github.fgrutsch.ui.catalog

import io.github.fgrutsch.catalog.Item
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface CatalogItemRepository {
    suspend fun search(query: String): List<Item.CatalogItem>
}

class ApiCatalogItemRepository(
    private val client: CatalogItemClient,
) : CatalogItemRepository {

    private val mutex = Mutex()
    private var cachedItems: List<Item.CatalogItem> = emptyList()

    override suspend fun search(query: String): List<Item.CatalogItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val items = mutex.withLock {
            if (cachedItems.isEmpty()) cachedItems = client.fetchAll()
            cachedItems
        }
        return items
            .filter { it.name.lowercase().contains(q) }
            .take(5)
    }
}
