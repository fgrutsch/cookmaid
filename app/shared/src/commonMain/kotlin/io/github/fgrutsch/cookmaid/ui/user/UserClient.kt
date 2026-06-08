package io.github.fgrutsch.cookmaid.ui.user

import io.github.fgrutsch.cookmaid.user.User
import io.github.fgrutsch.cookmaid.ui.auth.ApiClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.post

/**
 * HTTP client for user account operations.
 */
interface UserClient {
    /**
     * Registers the authenticated user if needed and returns the user record.
     */
    suspend fun getOrCreateUser(): User

    /**
     * Permanently deletes the authenticated user's account and all associated data.
     */
    suspend fun deleteAccount()
}

class ApiUserClient(
    private val apiClient: ApiClient,
) : UserClient {
    override suspend fun getOrCreateUser(): User =
        apiClient.httpClient.post("/api/users/me").body()

    override suspend fun deleteAccount() {
        apiClient.httpClient.delete("/api/users/me")
    }
}
