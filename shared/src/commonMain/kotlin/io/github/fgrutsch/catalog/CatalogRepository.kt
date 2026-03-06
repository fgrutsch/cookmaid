package io.github.fgrutsch.catalog

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface CatalogRepository {
    val items: StateFlow<List<Item.CategorizedItem>>
    suspend fun search(query: String): List<Item.CategorizedItem>
}

class InMemoryCatalogRepository : CatalogRepository {
    private val _items = MutableStateFlow(defaultCatalogItems())
    override val items: StateFlow<List<Item.CategorizedItem>> = _items.asStateFlow()

    override suspend fun search(query: String): List<Item.CategorizedItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return _items.value
            .filter { it.name.lowercase().contains(q) }
            .take(5)
    }
}

private fun defaultCatalogItems(): List<Item.CategorizedItem> {
    val f = DefaultCategoryIds.FRUITS
    val v = DefaultCategoryIds.VEGETABLES
    val d = DefaultCategoryIds.DAIRY
    val m = DefaultCategoryIds.MEAT
    val b = DefaultCategoryIds.BAKERY
    val bv = DefaultCategoryIds.BEVERAGES
    val s = DefaultCategoryIds.SNACKS
    val o = DefaultCategoryIds.OTHER

    return listOf(
        Item.CategorizedItem(id = "item-apples", name = "Apples", category = f),
        Item.CategorizedItem(id = "item-bananas", name = "Bananas", category = f),
        Item.CategorizedItem(id = "item-lemons", name = "Lemons", category = f),
        Item.CategorizedItem(id = "item-onions", name = "Onions", category = v),
        Item.CategorizedItem(id = "item-tomatoes", name = "Tomatoes", category = v),
        Item.CategorizedItem(id = "item-potatoes", name = "Potatoes", category = v),
        Item.CategorizedItem(id = "item-carrots", name = "Carrots", category = v),
        Item.CategorizedItem(id = "item-milk", name = "Milk", category = d),
        Item.CategorizedItem(id = "item-butter", name = "Butter", category = d),
        Item.CategorizedItem(id = "item-cheese", name = "Cheese", category = d),
        Item.CategorizedItem(id = "item-eggs", name = "Eggs", category = d),
        Item.CategorizedItem(id = "item-chicken", name = "Chicken", category = m),
        Item.CategorizedItem(id = "item-ground-beef", name = "Ground Beef", category = m),
        Item.CategorizedItem(id = "item-bread", name = "Bread", category = b),
        Item.CategorizedItem(id = "item-flour", name = "Flour", category = b),
        Item.CategorizedItem(id = "item-water", name = "Water", category = bv),
        Item.CategorizedItem(id = "item-coffee", name = "Coffee", category = bv),
        Item.CategorizedItem(id = "item-chips", name = "Chips", category = s),
        Item.CategorizedItem(id = "item-salt", name = "Salt", category = o),
        Item.CategorizedItem(id = "item-olive-oil", name = "Olive Oil", category = o),
        Item.CategorizedItem(id = "item-sugar", name = "Sugar", category = o),
    )
}
