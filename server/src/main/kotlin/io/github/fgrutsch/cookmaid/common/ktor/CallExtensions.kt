package io.github.fgrutsch.cookmaid.common.ktor

import io.github.fgrutsch.cookmaid.common.SupportedLocale
import io.github.fgrutsch.cookmaid.user.UserId
import io.github.fgrutsch.cookmaid.user.UserService
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.util.AttributeKey
import org.koin.ktor.ext.get

private val UserIdKey = AttributeKey<UserId>("userId")

/**
 * Extracts the authenticated [UserId] from the JWT principal, caching it on the call attributes.
 *
 * @return the authenticated user's id.
 * @throws IllegalArgumentException if the JWT principal is missing.
 * @throws UserNotRegisteredException if the authenticated subject has no matching user row.
 */
suspend fun ApplicationCall.userId(): UserId {
    val cached = attributes.getOrNull(UserIdKey)
    if (cached != null) return cached

    val subject = requireNotNull(principal<JWTPrincipal>()) { "JWT principal missing" }.payload.subject
    val userService = application.get<UserService>()
    val userId = userService.findIdByOidcSubject(subject) ?: throw UserNotRegisteredException()
    attributes.put(UserIdKey, userId)
    return userId
}

/**
 * Extracts the [SupportedLocale] from the Accept-Language header, falling back to English.
 */
fun ApplicationCall.locale(): SupportedLocale =
    SupportedLocale.fromCode(request.headers[HttpHeaders.AcceptLanguage]?.take(2) ?: "en")
