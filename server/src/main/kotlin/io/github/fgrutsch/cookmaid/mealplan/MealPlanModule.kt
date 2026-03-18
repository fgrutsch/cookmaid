package io.github.fgrutsch.cookmaid.mealplan

import io.github.fgrutsch.cookmaid.common.ktor.localDate
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

val mealPlanModule = module {
    singleOf(::PostgresMealPlanRepository) bind MealPlanRepository::class
    singleOf(::MealPlanService)
}

fun Route.mealPlanRoutes() {
    val service by inject<MealPlanService>()

    route("/meal-plan") {

        get {
            val from = call.request.queryParameters.localDate("from")
            val to = call.request.queryParameters.localDate("to")
            call.respond(service.findByUser(call.userId(), from, to))
        }

        post {
            val body = call.receive<CreateMealPlanItemRequest>()
            val item = service.create(call.userId(), body.day, body.recipeId, body.note)
            call.respond(HttpStatusCode.Created, item)
        }

        route("/{id}") {

            put {
                val id = call.parameters.uuid("id")
                val body = call.receive<UpdateMealPlanItemRequest>()
                if (!service.update(call.userId(), id, body.day, body.note)) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }

            delete {
                val id = call.parameters.uuid("id")
                if (!service.delete(call.userId(), id)) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
