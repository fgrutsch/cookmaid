package io.github.fgrutsch.cookmaid.recipe.importer

import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient

private const val NUMBER = """(?:\d+(?:[.,]\d+)?(?:\s*/\s*\d+)?|[½⅓⅔¼¾⅛⅜⅝⅞])"""
private const val AMOUNT = """$NUMBER(?:\s*$NUMBER)?(?:\s*[-–]\s*$NUMBER)?"""
private val UNITS = listOf(
    "g", "kg", "mg", "ml", "cl", "dl", "l", "el", "tl", "msp", "prise", "prisen", "pck", "päckchen",
    "packung", "dose", "dosen", "becher", "bund", "stück", "stk", "zehe", "zehen", "scheibe", "scheiben",
    "tasse", "tassen", "tbsp", "tsp", "tablespoons?", "teaspoons?", "cups?", "oz", "lbs?", "pinch",
    "cloves?", "cans?", "slices?",
).joinToString("|")
private val INGREDIENT = Regex(
    """^\s*($AMOUNT(?:\s*(?:$UNITS)\.?(?=\s))?)\s+(.+)$""",
    RegexOption.IGNORE_CASE,
)

/**
 * Splits a free-text ingredient line such as `"200 g Butter"` into a quantity
 * (`"200 g"`) and a name (`"Butter"`). Lines without a leading amount keep the
 * whole text as the name and no quantity.
 */
fun parseIngredient(text: String): RecipeIngredient {
    val match = INGREDIENT.find(text.trim())
    return if (match == null) {
        RecipeIngredient(Item.FreeText(text.trim()))
    } else {
        RecipeIngredient(Item.FreeText(match.groupValues[2].trim()), match.groupValues[1].trim())
    }
}
