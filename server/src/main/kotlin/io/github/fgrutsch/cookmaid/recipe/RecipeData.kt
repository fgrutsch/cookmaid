package io.github.fgrutsch.cookmaid.recipe

data class RecipeData(
    val name: String,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>,
    val tags: List<String>,
)

fun CreateRecipeRequest.toData() = RecipeData(name, ingredients, steps, tags)
fun UpdateRecipeRequest.toData() = RecipeData(name, ingredients, steps, tags)
