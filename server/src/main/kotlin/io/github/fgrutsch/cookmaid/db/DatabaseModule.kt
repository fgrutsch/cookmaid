package io.github.fgrutsch.cookmaid.db

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.config.ApplicationConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.dsl.module

private val logger = KotlinLogging.logger {}

val databaseModule = module {
    single<Database>(createdAtStart = true) { createDatabase(get()) }
}

private fun createDatabase(config: ApplicationConfig): Database {
    val url = config.property("database.url").getString()
    val user = config.property("database.user").getString()
    val password = config.property("database.password").getString()

    logger.info { "Running Flyway migrations..." }
    Flyway.configure()
        .dataSource(url, user, password)
        .load()
        .migrate()

    logger.info { "Connecting to database..." }
    return Database.connect(url, driver = "org.postgresql.Driver", user = user, password = password)
}
