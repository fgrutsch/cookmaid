package io.github.fgrutsch.cookmaid.shopping

import io.github.fgrutsch.cookmaid.common.ktor.locale
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
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ktor.ext.inject

val shoppingModule = module {
    singleOf(::PostgresShoppingListRepository) bind ShoppingListRepository::class
    singleOf(::ShoppingListService)
}

fun Route.shoppingRoutes() {
    val service by inject<ShoppingListService>()

    route("/shopping-lists") {
        get {
            call.respond(service.findLists(call.userId()))
        }
        post {
            val body = call.receive<CreateListRequest>()
            val list = service.createList(call.userId(), body.name)
            call.respond(HttpStatusCode.Created, list)
        }
        route("/{listId}") {
            put {
                val listId = call.parameters.uuid("listId")
                val body = call.receive<UpdateListRequest>()
                if (!service.updateList(call.userId(), listId, body.name)) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
            delete {
                val listId = call.parameters.uuid("listId")
                when (service.deleteList(call.userId(), listId)) {
                    DeleteListResult.Deleted -> call.respond(HttpStatusCode.NoContent)
                    DeleteListResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                    DeleteListResult.CannotDeleteDefault -> call.respond(HttpStatusCode.Conflict)
                }
            }
            shoppingItemRoutes(service)
        }
    }
}

@Suppress("LongMethod")
private fun Route.shoppingItemRoutes(service: ShoppingListService) {
    route("/items") {
        get {
            val listId = call.parameters.uuid("listId")
            val items = service.findItemsByListId(call.userId(), listId, call.locale())
            if (items == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(items)
            }
        }
        post {
            val listId = call.parameters.uuid("listId")
            val body = call.receive<CreateShoppingItemRequest>()
            val created = service.addItem(
                call.userId(), listId, body.catalogItemId, body.freeTextName, body.quantity, call.locale(),
            )
            if (created == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(HttpStatusCode.Created, created)
            }
        }
        post("/batch") {
            val listId = call.parameters.uuid("listId")
            val body = call.receive<BatchAddItemsRequest>()
            val created = service.addItems(call.userId(), listId, body.items, call.locale())
            if (created == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(HttpStatusCode.Created, created)
            }
        }
        delete {
            val listId = call.parameters.uuid("listId")
            val checked = call.request.queryParameters["checked"]?.toBooleanStrictOrNull()
            if (checked == true) {
                if (!service.deleteCheckedItems(call.userId(), listId)) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            } else {
                call.respond(HttpStatusCode.BadRequest)
            }
        }
        route("/{itemId}") {
            put {
                val itemId = call.parameters.uuid("itemId")
                val body = call.receive<UpdateItemRequest>()
                if (!service.updateItem(call.userId(), itemId, body.quantity, body.checked)) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
            delete {
                val itemId = call.parameters.uuid("itemId")
                if (!service.deleteItem(call.userId(), itemId)) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
