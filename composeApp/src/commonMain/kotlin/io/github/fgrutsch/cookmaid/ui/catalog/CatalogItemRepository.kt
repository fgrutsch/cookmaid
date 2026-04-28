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

    /**
     * Finds a catalog item whose localized name exactly matches [name] (case-insensitive, trimmed).
     *
     * @param name the text to match exactly against catalog item names.
     * @return the first matching [Item.Catalog], or null if no exact match exists.
     */
    suspend fun findExactMatch(name: String): Item.Catalog?
}

class ApiCatalogItemRepository(
    private val client: CatalogItemClient,
) : CatalogItemRepository {

    private val mutex = Mutex()
    private var cachedItems: List<Item.Catalog> = emptyList()

    override suspend fun search(query: String): List<Item.Catalog> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val items = ensureCacheLoaded()
        return items
            .filter { it.name.lowercase().contains(q) }
            .take(MAX_SEARCH_RESULTS)
    }

    override suspend fun findExactMatch(name: String): Item.Catalog? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        val items = ensureCacheLoaded()
        return items.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
    }

    private suspend fun ensureCacheLoaded(): List<Item.Catalog> = mutex.withLock {
        if (cachedItems.isEmpty()) cachedItems = client.fetchAll()
        cachedItems
    }

    companion object {
        private const val MAX_SEARCH_RESULTS = 5
    }
}
