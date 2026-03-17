package io.github.fgrutsch.cookmaid.recipe

import io.github.fgrutsch.cookmaid.common.ktor.userId
import io.github.fgrutsch.cookmaid.common.ktor.uuid
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlin.time.Instant
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ktor.ext.inject

val recipeModule = module {
    singleOf(::PostgresRecipeRepository) bind RecipeRepository::class
    singleOf(::RecipeService)
}

fun Route.recipeRoutes() {
    val service by inject<RecipeService>()

    route("/recipes") {

        get {
            val cursor = call.request.queryParameters["cursor"]?.let { Instant.fromEpochMilliseconds(it.toLong()) }
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val search = call.request.queryParameters["search"]
            val tag = call.request.queryParameters["tag"]
            call.respond(service.findByUser(call.userId(), cursor, limit.coerceIn(1, 100), search, tag))
        }

        get("/tags") {
            call.respond(TagsResponse(items = service.findTagsByUser(call.userId())))
        }

        post {
            val body = call.receive<CreateRecipeRequest>()
            val recipe = service.create(call.userId(), body.name, body.ingredients, body.steps, body.tags)
            call.respond(HttpStatusCode.Created, recipe)
        }

        route("/{recipeId}") {

            get {
                val recipeId = call.parameters.uuid("recipeId")
                val recipe = service.findById(call.userId(), recipeId)
                if (recipe == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(recipe)
                }
            }

            put {
                val recipeId = call.parameters.uuid("recipeId")
                val body = call.receive<UpdateRecipeRequest>()
                if (!service.update(call.userId(), recipeId, body.name, body.ingredients, body.steps, body.tags)) {
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
