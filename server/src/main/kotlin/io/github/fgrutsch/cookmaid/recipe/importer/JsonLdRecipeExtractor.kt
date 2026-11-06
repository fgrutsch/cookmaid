package io.github.fgrutsch.cookmaid.recipe.importer

import io.github.fgrutsch.cookmaid.recipe.RecipeRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * [RecipeExtractor] that reads a schema.org `Recipe` from a page's JSON-LD blocks — the structured data
 * most recipe sites embed for search engines. Handles `@graph` wrappers,
 * `mainEntity`, array `@type`, and the common `recipeInstructions` /
 * `recipeYield` shapes. Ingredients come back as free text split by
 * [IngredientParser]; the caller length-checks the result.
 */
class JsonLdRecipeExtractor(
    private val config: RecipeImportConfig,
    private val ingredientParser: IngredientParser,
) : RecipeExtractor {

    override val priority = 0

    private val yieldNumber = Regex("""\d+""")

    /**
     * @return the first recipe found in [document], or null when the page has none.
     */
    override suspend fun extract(document: Document): RecipeRequest? =
        document.select("script[type^=application/ld+json]").asSequence()
            .mapNotNull { runCatching { Json.parseToJsonElement(it.data()) }.getOrNull() }
            .mapNotNull { findRecipe(it) }
            .firstNotNullOfOrNull { toRequest(it) }

    private fun findRecipe(element: JsonElement?): JsonObject? = when (element) {
        is JsonArray -> element.firstNotNullOfOrNull { findRecipe(it) }
        is JsonObject -> element.takeIf { isRecipe(it) }
            ?: findRecipe(element["@graph"])
            ?: findRecipe(element["mainEntity"])
        else -> null
    }

    private fun isRecipe(obj: JsonObject): Boolean = when (val type = obj["@type"]) {
        is JsonPrimitive -> type.contentOrNull == "Recipe"
        is JsonArray -> type.any { (it as? JsonPrimitive)?.contentOrNull == "Recipe" }
        else -> false
    }

    private fun toRequest(recipe: JsonObject): RecipeRequest? {
        val name = text(recipe["name"]) ?: return null
        return RecipeRequest(
            name = name,
            description = text(recipe["description"]),
            ingredients = strings(recipe["recipeIngredient"]).take(config.maxItems).map(ingredientParser::parse),
            steps = instructions(recipe["recipeInstructions"]),
            servings = servings(recipe["recipeYield"]),
        )
    }

    private fun text(element: JsonElement?): String? = clean((element as? JsonPrimitive)?.contentOrNull)

    private fun clean(raw: String?): String? =
        raw?.let { Jsoup.parseBodyFragment(it).text() }?.takeIf { it.isNotBlank() }

    private fun strings(element: JsonElement?): List<String> = when (element) {
        is JsonArray -> element.mapNotNull { text(it) }
        else -> listOfNotNull(text(element))
    }

    private fun instructions(element: JsonElement?): List<String> = when (element) {
        is JsonArray -> element.flatMap { instructions(it) }
        is JsonObject -> element["itemListElement"]?.let { instructions(it) }
            ?: listOfNotNull(text(element["text"]) ?: text(element["name"]))
        is JsonPrimitive -> element.contentOrNull.orEmpty().lines().mapNotNull(::clean)
        else -> emptyList()
    }

    private fun servings(element: JsonElement?): Int? = when (element) {
        is JsonArray -> element.firstNotNullOfOrNull { servings(it) }
        is JsonPrimitive -> element.contentOrNull?.let { yieldNumber.find(it)?.value?.toIntOrNull() }
        else -> null
    }
}
