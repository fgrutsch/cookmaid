package io.github.fgrutsch.ui.common

import io.github.fgrutsch.recipe.RecipeIngredient
import io.github.fgrutsch.shopping.ShoppingItem
import io.github.fgrutsch.shopping.ShoppingListRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
suspend fun addIngredientsToDefaultShoppingList(
    shoppingListRepository: ShoppingListRepository,
    ingredients: List<RecipeIngredient>,
) {
    val lists = shoppingListRepository.lists.value
    val targetListId = lists.find { it.default }?.id ?: lists.firstOrNull()?.id ?: return
    ingredients.forEach { ingredient ->
        shoppingListRepository.addItem(
            targetListId,
            ShoppingItem(id = Uuid.random().toString(), item = ingredient.item, quantity = ingredient.quantity),
        )
    }
}
