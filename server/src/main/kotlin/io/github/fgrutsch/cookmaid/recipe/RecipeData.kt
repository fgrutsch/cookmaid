package io.github.fgrutsch.cookmaid.recipe

data class RecipeData(
    val name: String,
    val description: String?,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>,
    val tags: List<String>,
    val servings: Int?,
)

fun CreateRecipeRequest.toData() = RecipeData(name, description, ingredients, steps, tags, servings)
fun UpdateRecipeRequest.toData() = RecipeData(name, description, ingredients, steps, tags, servings)
