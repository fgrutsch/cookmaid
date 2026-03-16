package io.github.fgrutsch.cookmaid.mealplan

import kotlinx.serialization.Serializable

@Serializable
sealed interface MealPlanItem {
    val id: String

    @Serializable
    data class RecipeItem(
        override val id: String,
        val recipeId: String,
    ) : MealPlanItem

    @Serializable
    data class NoteItem(
        override val id: String,
        val name: String,
    ) : MealPlanItem
}
