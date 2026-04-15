package io.github.fgrutsch.cookmaid.ui.catalog

import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.ui.auth.ApiClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * HTTP client for catalog-item endpoints. Interface-based so repository tests
 * can substitute a fake without constructing the full [ApiClient].
 */
interface CatalogItemClient {
    suspend fun fetchAll(): List<Item.Catalog>
}

class ApiCatalogItemClient(
    private val apiClient: ApiClient,
) : CatalogItemClient {
    override suspend fun fetchAll(): List<Item.Catalog> =
        apiClient.httpClient.get("/api/catalog-items").body()
}
