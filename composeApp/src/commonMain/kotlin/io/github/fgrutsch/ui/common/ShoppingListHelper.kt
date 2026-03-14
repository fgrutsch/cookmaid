package io.github.fgrutsch.ui.common

import io.github.fgrutsch.catalog.Item
import io.github.fgrutsch.recipe.RecipeIngredient
import io.github.fgrutsch.ui.shopping.ShoppingListRepository

suspend fun addIngredientsToDefaultShoppingList(
    shoppingListRepository: ShoppingListRepository,
    ingredients: List<RecipeIngredient>,
) {
    val lists = shoppingListRepository.cachedLists
    val targetListId = lists.find { it.default }?.id ?: lists.firstOrNull()?.id ?: return
    ingredients.forEach { ingredient ->
        val catalogItemId = (ingredient.item as? Item.CatalogItem)?.id
        val freeTextName = (ingredient.item as? Item.FreeTextItem)?.name
        shoppingListRepository.addItem(targetListId, catalogItemId, freeTextName, ingredient.quantity)
    }
}
