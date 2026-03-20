package io.github.fgrutsch.cookmaid.ui.catalog

import io.github.fgrutsch.cookmaid.catalog.Item
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface CatalogItemRepository {
    suspend fun search(query: String): List<Item.Catalog>
}

class ApiCatalogItemRepository(
    private val client: CatalogItemClient,
) : CatalogItemRepository {

    private val mutex = Mutex()
    private var cachedItems: List<Item.Catalog> = emptyList()

    override suspend fun search(query: String): List<Item.Catalog> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val items = mutex.withLock {
            if (cachedItems.isEmpty()) cachedItems = client.fetchAll()
            cachedItems
        }
        return items
            .filter { it.name.lowercase().contains(q) }
            .take(MAX_SEARCH_RESULTS)
    }

    companion object {
        private const val MAX_SEARCH_RESULTS = 5
    }
}
