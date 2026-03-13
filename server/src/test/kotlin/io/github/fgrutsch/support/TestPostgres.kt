package io.github.fgrutsch.support

import org.testcontainers.containers.PostgreSQLContainer

object TestPostgres {
    private val container = PostgreSQLContainer("postgres:18.3-alpine").apply {
        start()
    }

    val jdbcUrl: String get() = container.jdbcUrl
    val username: String get() = container.username
    val password: String get() = container.password
}
