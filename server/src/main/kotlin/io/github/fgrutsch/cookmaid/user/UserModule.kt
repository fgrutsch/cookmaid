package io.github.fgrutsch.cookmaid.user

import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ktor.ext.inject

val userModule = module {
    singleOf(::PostgresUserRepository) bind UserRepository::class
    singleOf(::UserService)
}

fun Route.userRoutes() {
    val service by inject<UserService>()

    route("/users") {
        post("/me") {
            val principal = requireNotNull(call.principal<JWTPrincipal>()) { "JWT principal missing" }
            val user = service.getOrCreate(oidcSubject = principal.payload.subject)
            call.respond(user)
        }
    }
}
