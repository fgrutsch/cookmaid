package io.github.fgrutsch.cookmaid.mealplan

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
import kotlinx.datetime.LocalDate
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ktor.ext.inject

val mealPlanModule = module {
    singleOf(::PostgresMealPlanRepository) bind ServerMealPlanRepository::class
    singleOf(::MealPlanService)
}

fun Route.mealPlanRoutes() {
    val service by inject<MealPlanService>()

    route("/meal-plan") {

        get {
            val from = LocalDate.parse(call.request.queryParameters["from"]!!)
            val to = LocalDate.parse(call.request.queryParameters["to"]!!)
            call.respond(service.findByUser(call.userId(), from, to))
        }

        post {
            val body = call.receive<CreateMealPlanItemRequest>()
            val item = service.create(call.userId(), body.dayDate, body.recipeId, body.note)
            call.respond(HttpStatusCode.Created, item)
        }

        route("/{id}") {

            put {
                val id = call.parameters.uuid("id")
                val body = call.receive<UpdateMealPlanItemRequest>()
                if (!service.update(call.userId(), id, body.dayDate, body.note)) {
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
