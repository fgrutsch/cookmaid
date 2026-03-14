package io.github.fgrutsch.catalog

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.dsl.module
import org.koin.ktor.ext.inject

val catalogModule = module {
    single<CatalogItemRepository> { PostgresCatalogItemRepository() }
}

fun Route.catalogRoutes() {
    val itemRepository by inject<CatalogItemRepository>()

    route("/catalog-items") {
        get {
            call.respond(itemRepository.findAll())
        }
    }
}
