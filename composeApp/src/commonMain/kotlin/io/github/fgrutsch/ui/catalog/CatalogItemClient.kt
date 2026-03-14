package io.github.fgrutsch.ui.catalog

import io.github.fgrutsch.ApiBaseUrl
import io.github.fgrutsch.catalog.Item
import io.github.fgrutsch.ui.auth.ApiClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class CatalogItemClient(
    private val baseUrl: ApiBaseUrl,
    private val apiClient: ApiClient,
) {
    suspend fun fetchAll(): List<Item.CatalogItem> =
        apiClient.httpClient.get("${baseUrl.value}/api/catalog-items").body()
}
