package io.github.fgrutsch.cookmaid.ui.common

import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.shopping.CreateShoppingItemRequest
import io.github.fgrutsch.cookmaid.ui.shopping.ShoppingListRepository

/**
 * Adds the given [ingredients] to the user's default shopping list.
 *
 * @param shoppingListRepository repository used to look up lists and add items.
 * @param ingredients the recipe ingredients to add as shopping items.
 */
suspend fun addIngredientsToDefaultShoppingList(
    shoppingListRepository: ShoppingListRepository,
    ingredients: List<RecipeIngredient>,
) {
    if (ingredients.isEmpty()) return
    val lists = shoppingListRepository.getLists()
    val targetListId = lists.find { it.default }?.id ?: lists.firstOrNull()?.id ?: return
    val items = ingredients.map { ingredient ->
        CreateShoppingItemRequest(
            catalogItemId = (ingredient.item as? Item.Catalog)?.id,
            freeTextName = (ingredient.item as? Item.FreeText)?.name,
            quantity = ingredient.quantity,
        )
    }
    shoppingListRepository.addItems(targetListId, items)
}
