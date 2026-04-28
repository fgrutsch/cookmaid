package io.github.fgrutsch.cookmaid

import io.github.fgrutsch.cookmaid.auth.AUTH_JWT
import io.github.fgrutsch.cookmaid.auth.configureAuth
import io.github.fgrutsch.cookmaid.catalog.catalogModule
import io.github.fgrutsch.cookmaid.catalog.catalogRoutes
import io.github.fgrutsch.cookmaid.common.ktor.ErrorResponse
import io.github.fgrutsch.cookmaid.common.ktor.UserNotRegisteredException
import io.github.fgrutsch.cookmaid.db.databaseModule
import io.github.fgrutsch.cookmaid.mealplan.mealPlanModule
import io.github.fgrutsch.cookmaid.mealplan.mealPlanRoutes
import io.github.fgrutsch.cookmaid.recipe.recipeModule
import io.github.fgrutsch.cookmaid.recipe.recipeRoutes
import io.github.fgrutsch.cookmaid.shopping.shoppingModule
import io.github.fgrutsch.cookmaid.shopping.shoppingRoutes
import io.github.fgrutsch.cookmaid.user.userModule
import io.github.fgrutsch.cookmaid.user.userRoutes
import io.github.fgrutsch.cookmaid.web.configureStaticFiles
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.hsts.HSTS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
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
    configureStatusPages()
    configureStaticFiles()
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
            recipeModule,
            mealPlanModule,
        )
    }
}

private fun Application.configureHttp() {
    val oidcIssuer = environment.config.property("oidc.issuer").getString()
    install(ContentNegotiation) { json() }
    install(DefaultHeaders) {
        header("X-Frame-Options", "DENY")
        header("X-Content-Type-Options", "nosniff")
        header(
            "Content-Security-Policy",
            "default-src 'self'; script-src 'self' 'unsafe-inline' 'wasm-unsafe-eval'; worker-src 'self' blob:; " +
                "style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; " +
                "connect-src 'self' $oidcIssuer; object-src 'none'",
        )
    }
    install(HSTS)
}

private fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<UserNotRegisteredException> { call, _ ->
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("user_not_registered"))
        }
        exception<MissingRequestParameterException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest)
        }
        exception<IllegalArgumentException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest)
        }
        exception<Throwable> { call, _ ->
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}

private fun Application.configureRouting() {
    routing {
        route("/api") {
            authenticate(AUTH_JWT) {
                userRoutes()
                catalogRoutes()
                shoppingRoutes()
                recipeRoutes()
                mealPlanRoutes()
            }
        }
    }
}
