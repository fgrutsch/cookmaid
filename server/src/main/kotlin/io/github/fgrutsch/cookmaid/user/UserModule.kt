package io.github.fgrutsch.cookmaid.user

import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.dsl.module
import org.koin.ktor.ext.inject

val userModule = module {
    single<UserRepository> { PostgresUserRepository() }
    single { UserService(get(), get()) }
}

fun Route.userRoutes() {
    val service by inject<UserService>()

    route("/users") {
        post("/me") {
            val principal = call.principal<JWTPrincipal>()!!
            val user = service.getOrCreate(oidcSubject = principal.payload.subject)
            call.respond(user)
        }
    }
}
