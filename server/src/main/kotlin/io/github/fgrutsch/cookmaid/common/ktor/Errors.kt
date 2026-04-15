package io.github.fgrutsch.cookmaid.common.ktor

import kotlinx.serialization.Serializable

/**
 * Signals that the JWT-authenticated caller has no corresponding user row.
 * Thrown by [ApplicationCall.userId] when a client hits a protected endpoint
 * before calling `POST /api/users/me`. Carries no message — the subject is
 * never included in the response.
 */
class UserNotRegisteredException : RuntimeException()

/**
 * Envelope for structured error responses. Currently only used for
 * `user_not_registered` so clients can detect the bootstrap case.
 */
@Serializable
data class ErrorResponse(val error: String)
