package io.github.fgrutsch.cookmaid.catalog

import io.github.fgrutsch.cookmaid.common.ktor.locale
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

/**
 * Catalog routes talk to the repository directly: the catalog is a global,
 * read-only, authz-free table. Introduce a `CatalogItemService` if/when any
 * of those stop being true (admin mutations, per-user favorites, auditing).
 */
fun Route.catalogRoutes() {
    val itemRepository by inject<CatalogItemRepository>()

    route("/catalog-items") {
        get {
            call.respond(itemRepository.findAll(call.locale()))
        }
    }
}
