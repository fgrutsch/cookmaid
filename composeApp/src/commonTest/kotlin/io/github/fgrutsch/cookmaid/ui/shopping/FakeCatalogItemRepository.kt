package io.github.fgrutsch.cookmaid.ui.shopping

import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.ui.catalog.CatalogItemRepository

class FakeCatalogItemRepository : CatalogItemRepository {

    var items: List<Item.Catalog> = emptyList()

    override suspend fun search(query: String): List<Item.Catalog> {
        if (query.isBlank()) return emptyList()
        return items.filter { it.name.lowercase().contains(query.lowercase()) }
    }

    override suspend fun findExactMatch(name: String): Item.Catalog? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        val key = trimmed.lowercase()
        return items.firstOrNull { it.name.lowercase() == key }
    }
}
