package io.github.fgrutsch.cookmaid.user

import io.github.fgrutsch.cookmaid.common.ktor.oidcSubject
import io.github.fgrutsch.cookmaid.common.ktor.userId
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
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
            val user = service.getOrCreate(oidcSubject = call.oidcSubject())
            call.respond(user)
        }
        delete("/me") {
            val userId = call.userId()
            service.delete(userId, call.oidcSubject())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
