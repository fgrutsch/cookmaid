package io.github.fgrutsch.cookmaid.recipe.importer

import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.common.ktor.RecipeImportException
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.recipe.RecipeRequest
import io.github.fgrutsch.cookmaid.user.UserId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI

/**
 * Turns a recipe URL into a draft [RecipeRequest] for the user to review. Nothing
 * is saved. schema.org data on the page is used first; the optional [llm] is only
 * asked when the page has none. Every result is length-capped before it leaves.
 *
 * @param fetchPage downloads a user-supplied URL (see [PageFetcher.fetch]).
 * @param llm the LLM fallback, or null when no LLM is configured.
 */
class RecipeImportService(
    private val fetchPage: suspend (String) -> String,
    private val llm: LlmRecipeExtractor?,
) {

    companion object {
        private const val MAX_NAME = 200
        private const val MAX_DESCRIPTION = 2_000
        private const val MAX_LINE = 500
        private const val MAX_ITEMS = 100
        private val SERVINGS = 1..100
    }

    private val logger = KotlinLogging.logger {}

    /**
     * Imports the recipe at [url] as a draft.
     *
     * @param userId the requesting user, for logging.
     * @param url the page to import.
     * @return a draft with free-text ingredients.
     * @throws RecipeImportException if the page cannot be fetched or holds no usable recipe.
     */
    suspend fun import(userId: UserId, url: String): RecipeRequest {
        val document = Jsoup.parse(fetchPage(url), url)
        val fromJsonLd = JsonLdRecipeExtractor.extract(document)?.let(::sanitize)
        val draft = fromJsonLd ?: llm?.extract(pageText(document))?.let(::sanitize)
        val host = URI(url.trim()).host
        if (draft == null) {
            logger.debug { "No recipe found for user ${userId.value} on $host" }
            throw RecipeImportException("No recipe found")
        }
        val source = if (fromJsonLd != null) "json-ld" else "llm"
        logger.info { "Imported recipe for user ${userId.value} from $host via $source" }
        return draft
    }

    private fun pageText(document: Document): String = "${document.title()}\n${document.body().text()}"

    private fun sanitize(recipe: RecipeRequest): RecipeRequest? {
        val ingredients = recipe.ingredients.mapNotNull { ingredient ->
            line((ingredient.item as? Item.FreeText)?.name)?.let {
                RecipeIngredient(Item.FreeText(it), line(ingredient.quantity))
            }
        }.take(MAX_ITEMS)
        val steps = recipe.steps.mapNotNull(::line).take(MAX_ITEMS)
        val name = recipe.name.trim().take(MAX_NAME)
        return if (name.isBlank() || (ingredients.isEmpty() && steps.isEmpty())) {
            null
        } else {
            RecipeRequest(
                name = name,
                description = recipe.description?.trim()?.take(MAX_DESCRIPTION)?.takeIf { it.isNotBlank() },
                ingredients = ingredients,
                steps = steps,
                servings = recipe.servings?.takeIf { it in SERVINGS },
            )
        }
    }

    private fun line(value: String?): String? = value?.trim()?.take(MAX_LINE)?.takeIf { it.isNotBlank() }
}
