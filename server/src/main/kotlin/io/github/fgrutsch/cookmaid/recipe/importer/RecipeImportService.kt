package io.github.fgrutsch.cookmaid.recipe.importer

import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.common.ktor.RecipeImportException
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.recipe.RecipeRequest
import io.github.fgrutsch.cookmaid.user.UserId
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.Url
import org.jsoup.Jsoup

/**
 * Turns a recipe URL into a draft [RecipeRequest] for the user to review. Nothing
 * is saved. Extractors are tried by ascending priority and the first usable result wins.
 * Every result is length-capped before it leaves.
 *
 * @param pageFetcher downloads the user-supplied URL.
 * @param extractors all available recipe extractors, in any order.
 */
class RecipeImportService(
    private val pageFetcher: PageFetcher,
    extractors: List<RecipeExtractor>,
) {

    companion object {
        private const val MAX_NAME = 200
        private const val MAX_DESCRIPTION = 2_000
        private const val MAX_LINE = 500
        private const val MAX_ITEMS = 100
        private val SERVINGS = 1..100
    }

    private val logger = KotlinLogging.logger {}
    private val extractors = extractors.sortedBy { it.priority }

    /**
     * Imports the recipe at [url] as a draft.
     *
     * @param userId the requesting user, for logging.
     * @param url the page to import.
     * @return a draft with free-text ingredients.
     * @throws RecipeImportException if the page cannot be fetched or holds no usable recipe.
     */
    suspend fun import(userId: UserId, url: String): RecipeRequest {
        val document = Jsoup.parse(pageFetcher.fetch(url), url)
        val host = Url(url.trim()).host
        val (extractor, draft) = extractors.firstNotNullOfOrNull { extractor ->
            extractor.extract(document)?.let(::sanitize)?.let { extractor to it }
        } ?: run {
            logger.debug { "No recipe found for user ${userId.value} on $host" }
            throw RecipeImportException("No recipe found")
        }
        logger.info { "Imported recipe for user ${userId.value} from $host via ${extractor::class.simpleName}" }
        return draft
    }

    private fun sanitize(recipe: RecipeRequest): RecipeRequest? {
        val ingredients = recipe.ingredients.mapNotNull { ingredient ->
            line((ingredient.item as? Item.FreeText)?.name)?.let {
                RecipeIngredient(Item.FreeText(it), line(ingredient.quantity))
            }
        }.take(MAX_ITEMS)
        val steps = recipe.steps.mapNotNull { line(it) }.take(MAX_ITEMS)
        val name = line(recipe.name, MAX_NAME)
        return if (name == null || (ingredients.isEmpty() && steps.isEmpty())) {
            null
        } else {
            RecipeRequest(
                name = name,
                description = line(recipe.description, MAX_DESCRIPTION),
                ingredients = ingredients,
                steps = steps,
                servings = recipe.servings?.takeIf { it in SERVINGS },
            )
        }
    }

    private fun line(value: String?, max: Int = MAX_LINE): String? =
        value?.trim()?.take(max)?.takeIf { it.isNotBlank() }
}
