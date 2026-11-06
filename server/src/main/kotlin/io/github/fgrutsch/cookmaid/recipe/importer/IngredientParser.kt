package io.github.fgrutsch.cookmaid.recipe.importer

import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient

/**
 * Splits a free-text ingredient line such as `"200 g Butter"` into a quantity
 * (`"200 g"`) and a name (`"Butter"`). The unit vocabulary comes from [config].
 * Lines without a leading amount keep the whole text as the name and no quantity.
 */
class IngredientParser(config: RecipeImportConfig) {

    companion object {
        // Possessive digit runs: a digit run can't be re-split between two NUMBER groups (catastrophic backtracking).
        private const val NUMBER = """(?:\d++(?:[.,]\d++)?(?:\s*/\s*\d++)?|[½⅓⅔¼¾⅛⅜⅝⅞])"""
        // Mixed numbers ("1 1/2", "1 and 1/2", "1 und 1/2") and ranges ("2-3").
        // "ca. 200 g", "je 1/2 TL", "about 2 cups".
        private const val APPROX = """(?:ca\.?|circa|etwa|je|approx\.?|about)"""
        private const val AMOUNT = """$NUMBER(?:\s*(?:(?:and|und)\s+)?$NUMBER)?(?:\s*[-–]\s*$NUMBER)?"""
    }

    private val units = config.ingredientUnits.joinToString("|") { Regex.escape(it) }
    // Unit with optional plural marker ("Zweig(e)") and bracketed amount ("cups (250g)").
    private val unit = """(?:\s*(?:$units)(?:\([a-zäöü]{1,2}\))?\.?(?=\s)(?:\s*\([^()]{1,20}\))?)?"""
    private val ingredient = Regex(
        """^\s*((?:$APPROX\s+)?$AMOUNT$unit)\s+(.+)$""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * @return the line as an ingredient with quantity, or with the whole text as name.
     */
    fun parse(text: String): RecipeIngredient {
        val line = text.trim()
        val match = ingredient.find(line)
        return if (match == null) {
            RecipeIngredient(Item.FreeText(line))
        } else {
            RecipeIngredient(Item.FreeText(match.groupValues[2].trim()), match.groupValues[1].trim())
        }
    }
}
