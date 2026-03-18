package io.github.fgrutsch.cookmaid.mealplan

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class MealPlanItemResponse(
    val id: Uuid,
    val day: LocalDate,
    val recipeId: Uuid? = null,
    val recipeName: String? = null,
    val note: String? = null,
)

@Serializable
data class CreateMealPlanItemRequest(
    val day: LocalDate,
    val recipeId: Uuid? = null,
    val note: String? = null,
)

@Serializable
data class UpdateMealPlanItemRequest(
    val day: LocalDate? = null,
    val note: String? = null,
)
