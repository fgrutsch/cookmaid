package io.github.fgrutsch.cookmaid.ui.catalog

import io.github.fgrutsch.cookmaid.catalog.Item
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repository for searching catalog items.
 */
interface CatalogItemRepository {
    /**
     * Searches for catalog items matching the given [query].
     *
     * @param query the search text to match against item names.
     * @return list of matching [Item.Catalog] entries.
     */
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

    /**
     * Drops the in-memory catalog cache. The next [search] call will
     * re-fetch from the server. Called by `SessionCleaner` on logout
     * for consistency with the other repos; catalog items are global,
     * so this is staleness prevention rather than a security concern.
     */
    suspend fun clear(): Unit = mutex.withLock {
        cachedItems = emptyList()
    }

    companion object {
        private const val MAX_SEARCH_RESULTS = 5
    }
}
