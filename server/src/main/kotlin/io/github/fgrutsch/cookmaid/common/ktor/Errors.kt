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
 * Signals that a recipe could not be imported from a URL (unreachable or
 * blocked page, no recipe found, LLM failure). [reason] is a fixed,
 * developer-facing description for logs — it must never contain the URL or
 * page content. Mapped to 422 with an empty body.
 */
class RecipeImportException(reason: String, cause: Throwable? = null) : RuntimeException(reason, cause)

/**
 * Envelope for structured error responses. Currently only used for
 * `user_not_registered` so clients can detect the bootstrap case.
 */
@Serializable
data class ErrorResponse(val error: String)
