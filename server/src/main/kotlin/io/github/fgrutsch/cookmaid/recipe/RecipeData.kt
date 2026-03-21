package io.github.fgrutsch.cookmaid.recipe

data class RecipeData(
    val name: String,
    val description: String?,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>,
    val tags: List<String>,
)

fun CreateRecipeRequest.toData() = RecipeData(name, description, ingredients, steps, tags)
fun UpdateRecipeRequest.toData() = RecipeData(name, description, ingredients, steps, tags)
