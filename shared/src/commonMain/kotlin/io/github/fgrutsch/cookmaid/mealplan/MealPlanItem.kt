package io.github.fgrutsch.cookmaid.mealplan

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
sealed interface MealPlanItem {
    val id: Uuid
    val day: LocalDate

    @Serializable
    @SerialName("recipe")
    data class Recipe(
        override val id: Uuid,
        override val day: LocalDate,
        val recipeId: Uuid,
        val recipeName: String,
    ) : MealPlanItem

    @Serializable
    @SerialName("note")
    data class Note(
        override val id: Uuid,
        override val day: LocalDate,
        val name: String,
    ) : MealPlanItem
}
