package io.github.fgrutsch.catalog

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ItemCategoryRepository {
    val categories: StateFlow<List<ItemCategory>>
    suspend fun getById(id: String): ItemCategory?
}

class InMemoryItemCategoryRepository : ItemCategoryRepository {
    private val _categories = MutableStateFlow(defaultCategories())
    override val categories: StateFlow<List<ItemCategory>> = _categories.asStateFlow()

    override suspend fun getById(id: String): ItemCategory? {
        return _categories.value.find { it.id == id }
    }
}

object DefaultCategoryIds {
    const val FRUITS = "cat-fruits"
    const val VEGETABLES = "cat-vegetables"
    const val DAIRY = "cat-dairy"
    const val MEAT = "cat-meat"
    const val BAKERY = "cat-bakery"
    const val BEVERAGES = "cat-beverages"
    const val SNACKS = "cat-snacks"
    const val OTHER = "cat-other"
}

private fun defaultCategories(): List<ItemCategory> = listOf(
    ItemCategory(id = DefaultCategoryIds.FRUITS, name = "Fruits"),
    ItemCategory(id = DefaultCategoryIds.VEGETABLES, name = "Vegetables"),
    ItemCategory(id = DefaultCategoryIds.DAIRY, name = "Dairy"),
    ItemCategory(id = DefaultCategoryIds.MEAT, name = "Meat"),
    ItemCategory(id = DefaultCategoryIds.BAKERY, name = "Bakery"),
    ItemCategory(id = DefaultCategoryIds.BEVERAGES, name = "Beverages"),
    ItemCategory(id = DefaultCategoryIds.SNACKS, name = "Snacks"),
    ItemCategory(id = DefaultCategoryIds.OTHER, name = "Other"),
)
