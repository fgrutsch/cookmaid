package io.github.fgrutsch.user

import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.dsl.module
import org.koin.ktor.ext.inject

val userModule = module {
    single<UserRepository> { InMemoryUserRepository() }
}

fun Route.userRoutes() {
    val repository by inject<UserRepository>()

    route("/users") {
        post("/me") {
            val principal = call.principal<JWTPrincipal>()!!
            val sub = principal.payload.subject
            val user = repository.findOrCreate(oidcSubject = sub)
            call.respond(user)
        }
    }
}
