package io.github.fgrutsch.cookmaid.ui.user

import io.github.fgrutsch.cookmaid.ApiBaseUrl
import io.github.fgrutsch.cookmaid.auth.User
import io.github.fgrutsch.cookmaid.ui.auth.ApiClient
import io.ktor.client.call.body
import io.ktor.client.request.post

class UserClient(
    private val baseUrl: ApiBaseUrl,
    private val apiClient: ApiClient,
) {
    suspend fun getOrCreateUser(): User =
        apiClient.httpClient.post("${baseUrl.value}/api/users/me").body()
}
