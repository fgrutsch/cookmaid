package io.github.fgrutsch.cookmaid.mealplan

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
sealed interface MealPlanItem {
    val id: Uuid

    @Serializable
    data class RecipeItem(
        override val id: Uuid,
        val recipeId: Uuid,
    ) : MealPlanItem

    @Serializable
    data class NoteItem(
        override val id: Uuid,
        val name: String,
    ) : MealPlanItem
}
