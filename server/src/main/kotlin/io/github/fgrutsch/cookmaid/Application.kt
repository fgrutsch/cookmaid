package io.github.fgrutsch.cookmaid

import io.github.fgrutsch.cookmaid.auth.AUTH_JWT
import io.github.fgrutsch.cookmaid.auth.configureAuth
import io.github.fgrutsch.cookmaid.catalog.catalogModule
import io.github.fgrutsch.cookmaid.catalog.catalogRoutes
import io.github.fgrutsch.cookmaid.db.databaseModule
import io.github.fgrutsch.cookmaid.shopping.shoppingModule
import io.github.fgrutsch.cookmaid.shopping.shoppingRoutes
import io.github.fgrutsch.cookmaid.user.userModule
import io.github.fgrutsch.cookmaid.user.userRoutes
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureDI()
    configureAuth()
    configureHttp()
    configureRouting()
}

private fun Application.configureDI() {
    install(Koin) {
        slf4jLogger()
        modules(
            module { single<ApplicationConfig> { environment.config } },
            databaseModule,
            userModule,
            catalogModule,
            shoppingModule,
        )
    }
}

private fun Application.configureHttp() {
    install(ContentNegotiation) { json() }
}

private fun Application.configureRouting() {
    routing {
        route("/api") {
            authenticate(AUTH_JWT) {
                userRoutes()
                catalogRoutes()
                shoppingRoutes()
            }
        }
    }
}
