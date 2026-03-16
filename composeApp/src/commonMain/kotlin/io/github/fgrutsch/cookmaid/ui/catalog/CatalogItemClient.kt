package io.github.fgrutsch.cookmaid.ui.catalog

import io.github.fgrutsch.cookmaid.ApiBaseUrl
import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.ui.auth.ApiClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class CatalogItemClient(
    private val baseUrl: ApiBaseUrl,
    private val apiClient: ApiClient,
) {
    suspend fun fetchAll(): List<Item.CatalogItem> =
        apiClient.httpClient.get("${baseUrl.value}/api/catalog-items").body()
}
