package io.github.fgrutsch.cookmaid.web

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import java.io.File

fun Application.configureStaticFiles() {
    val webDir = environment.config.property("web.dir").getString()
    routing {
        staticFiles("/", File(webDir)) {
            default("index.html")
        }
    }
}
