package io.github.fgrutsch.cookmaid.recipe.importer

import io.github.fgrutsch.cookmaid.recipe.RecipeRequest
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * [RecipeExtractor] for pages that mark the schema.org `Recipe` up as Microdata — `itemprop`
 * attributes on the visible HTML instead of a JSON-LD block (e.g. ichkoche.at). Only properties
 * that belong to the recipe itself are read, not those of nested items such as the author.
 */
class MicrodataRecipeExtractor(
    private val config: RecipeImportConfig,
    private val ingredientParser: IngredientParser,
) : RecipeExtractor {

    override val priority = 10

    private val yieldNumber = Regex("""\d+""")

    override suspend fun extract(document: Document): RecipeRequest? =
        document.selectFirst("[itemscope][itemtype~=(?i)schema\\.org/Recipe$]")?.let(::toRequest)

    private fun toRequest(recipe: Element): RecipeRequest? {
        val name = values(recipe, "name").firstOrNull() ?: return null
        return RecipeRequest(
            name = name,
            description = values(recipe, "description").firstOrNull(),
            ingredients = (values(recipe, "recipeIngredient") + values(recipe, "ingredients"))
                .take(config.maxItems)
                .map(ingredientParser::parse),
            steps = props(recipe, "recipeInstructions").flatMap(::steps).take(config.maxItems),
            servings = values(recipe, "recipeYield")
                .firstNotNullOfOrNull { yieldNumber.find(it)?.value?.toIntOrNull() },
        )
    }

    // Properties whose nearest item scope is the recipe, so a nested author or rating name is skipped.
    private fun props(recipe: Element, name: String): List<Element> =
        recipe.select("[itemprop~=(^|\\s)$name(\\s|$)]").filter { prop ->
            prop.parents().firstOrNull { it.hasAttr("itemscope") } == recipe
        }

    private fun values(recipe: Element, name: String): List<String> =
        props(recipe, name).mapNotNull { value(it) }

    private fun value(element: Element): String? =
        (if (element.tagName() == "meta") element.attr("content") else element.text()).trim().takeIf { it.isNotBlank() }

    private fun steps(instructions: Element): List<String> {
        val parts = instructions.select("[itemprop=text]").ifEmpty { instructions.select("li") }
        return parts.mapNotNull { value(it) }.ifEmpty { listOfNotNull(value(instructions)) }
    }
}
