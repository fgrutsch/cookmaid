package io.github.fgrutsch.cookmaid.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.onClose
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module
import javax.sql.DataSource

private const val POOL_MAX_SIZE = 10
private const val POOL_MIN_IDLE = 2

val databaseModule = module {
    single<DataSource> { createDataSource(get()) } withOptions {
        createdAtStart()
        onClose { (it as? HikariDataSource)?.close() }
    }
    single<Database>(createdAtStart = true) { createDatabase(get()) }
}

/**
 * Creates a pooled [HikariDataSource] and runs Flyway migrations against it.
 *
 * @param config the Ktor application config supplying `database.url`, `database.user`,
 *   and `database.password`.
 * @return the [DataSource] backed by a HikariCP connection pool.
 */
private fun createDataSource(config: ApplicationConfig): DataSource {
    val url = config.property("database.url").getString()
    val user = config.property("database.user").getString()
    val password = config.property("database.password").getString()

    val dataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = url
            username = user
            this.password = password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = POOL_MAX_SIZE
            minimumIdle = POOL_MIN_IDLE
        },
    )

    Flyway.configure()
        .dataSource(dataSource)
        .load()
        .migrate()

    return dataSource
}

/**
 * Connects Exposed to the pooled [DataSource].
 *
 * @param dataSource the connection pool created by [createDataSource].
 * @return the [Database] instance backed by the pool.
 */
private fun createDatabase(dataSource: DataSource): Database {
    return Database.connect(dataSource)
}
