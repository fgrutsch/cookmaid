package io.github.fgrutsch.cookmaid.ui.user

import io.github.fgrutsch.cookmaid.user.User
import io.github.fgrutsch.cookmaid.ui.auth.ApiClient
import io.ktor.client.call.body
import io.ktor.client.request.post

class UserClient(
    private val apiClient: ApiClient,
) {
    suspend fun getOrCreateUser(): User =
        apiClient.httpClient.post("/api/users/me").body()
}
