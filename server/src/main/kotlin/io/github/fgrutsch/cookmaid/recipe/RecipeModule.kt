package io.github.fgrutsch.cookmaid.recipe

import io.github.fgrutsch.cookmaid.common.ktor.instant
import io.github.fgrutsch.cookmaid.common.ktor.int
import io.github.fgrutsch.cookmaid.common.ktor.locale
import io.github.fgrutsch.cookmaid.common.ktor.userId
import io.github.fgrutsch.cookmaid.common.ktor.uuid
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import kotlin.time.Instant

val recipeModule = module {
    singleOf(::PostgresRecipeRepository) bind RecipeRepository::class
    singleOf(::RecipeService)
}

private const val MIN_LIMIT = 1
private const val MAX_LIMIT = 100

fun Route.recipeRoutes() {
    val service by inject<RecipeService>()

    route("/recipes") {

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

        post {
            val body = call.receive<CreateRecipeRequest>()
            val recipe = service.create(call.userId(), body.toData(), call.locale())
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
                val body = call.receive<UpdateRecipeRequest>()
                if (!service.update(call.userId(), recipeId, body.toData())) {
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
