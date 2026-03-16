package io.github.fgrutsch.cookmaid.common.ktor

import io.github.fgrutsch.cookmaid.user.UserService
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.util.AttributeKey
import org.koin.ktor.ext.get
import kotlin.uuid.Uuid

private val UserIdKey = AttributeKey<Uuid>("userId")

suspend fun ApplicationCall.userId(): Uuid {
    val cached = attributes.getOrNull(UserIdKey)
    if (cached != null) return cached

    val subject = principal<JWTPrincipal>()!!.payload.subject
    val userService = application.get<UserService>()
    val userId = userService.findIdByOidcSubject(subject)
        ?: throw IllegalStateException("User not found for subject: $subject")
    attributes.put(UserIdKey, userId)
    return userId
}
