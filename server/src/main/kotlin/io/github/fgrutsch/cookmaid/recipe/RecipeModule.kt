package io.github.fgrutsch.cookmaid.recipe

import io.github.fgrutsch.cookmaid.common.ktor.instant
import io.github.fgrutsch.cookmaid.common.ktor.int
import io.github.fgrutsch.cookmaid.common.ktor.locale
import io.github.fgrutsch.cookmaid.common.ktor.userId
import io.github.fgrutsch.cookmaid.common.ktor.uuid
import io.github.fgrutsch.cookmaid.common.llm.LlmConfig
import io.github.fgrutsch.cookmaid.recipe.importer.HttpPageFetcher
import io.github.fgrutsch.cookmaid.recipe.importer.JsonLdRecipeExtractor
import io.github.fgrutsch.cookmaid.recipe.importer.LlmRecipeExtractor
import io.github.fgrutsch.cookmaid.recipe.importer.PageFetcher
import io.github.fgrutsch.cookmaid.recipe.importer.RecipeImportService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import kotlin.uuid.Uuid

val recipeModule = module {
    singleOf(::PostgresRecipeRepository) bind RecipeRepository::class
    singleOf(::RecipeService)
    single<PageFetcher> { HttpPageFetcher() }
    single {
        // Priority order: schema.org data first, the optional LLM only as fallback.
        val llm = LlmConfig.from(get())?.let { config -> LlmRecipeExtractor(config) }
        val extractors = listOfNotNull(JsonLdRecipeExtractor, llm)
        RecipeImportService(get(), extractors)
    }
}

private fun Route.recipeImportRoute() {
    val importService by inject<RecipeImportService>()

    post("/import") {
        val body = call.receive<ImportRecipeRequest>()
        require(body.url.isNotBlank()) { "url is required" }
        call.respond(importService.import(call.userId(), body.url))
    }
}

private const val MIN_LIMIT = 1
private const val MAX_LIMIT = 100

@Suppress("LongMethod")
fun Route.recipeRoutes() {
    val service by inject<RecipeService>()

    route("/recipes") {
        recipeImportRoute()

        get {
            val cursor = call.request.queryParameters.instant("cursor")
            val limit = call.request.queryParameters.int("limit") ?: DEFAULT_RECIPE_PAGE_SIZE
            val search = call.request.queryParameters["search"]
            val tag = call.request.queryParameters["tag"]
            call.respond(
                service.find(call.userId(), cursor, limit.coerceIn(MIN_LIMIT, MAX_LIMIT), search, tag, call.locale()),
            )
        }

        get("/tags") {
            call.respond(TagsResponse(items = service.findTags(call.userId())))
        }

        get("/random") {
            val tag = call.request.queryParameters["tag"]
            val excludeIds = call.request.queryParameters["excludeIds"]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.map { Uuid.parse(it.trim()) }
                .orEmpty()
            val recipe = service.findRandom(call.userId(), tag, excludeIds, call.locale())
            if (recipe != null) call.respond(recipe) else call.respond(HttpStatusCode.NotFound)
        }

        post {
            val body = call.receive<RecipeRequest>()
            val recipe = service.create(call.userId(), body, call.locale())
            call.respond(HttpStatusCode.Created, recipe)
        }

        route("/{recipeId}") {

            get {
                val recipeId = call.parameters.uuid("recipeId")
                val recipe = service.findById(call.userId(), recipeId, call.locale())
                if (recipe == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(recipe)
                }
            }

            put {
                val recipeId = call.parameters.uuid("recipeId")
                val body = call.receive<RecipeRequest>()
                if (!service.update(call.userId(), recipeId, body)) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }

            delete {
                val recipeId = call.parameters.uuid("recipeId")
                if (!service.delete(call.userId(), recipeId)) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
