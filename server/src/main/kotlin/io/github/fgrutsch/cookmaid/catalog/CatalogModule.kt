package io.github.fgrutsch.cookmaid.catalog

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ktor.ext.inject

val catalogModule = module {
    singleOf(::PostgresCatalogItemRepository) bind CatalogItemRepository::class
}

fun Route.catalogRoutes() {
    val itemRepository by inject<CatalogItemRepository>()

    route("/catalog-items") {
        get {
            call.respond(itemRepository.findAll())
        }
    }
}
