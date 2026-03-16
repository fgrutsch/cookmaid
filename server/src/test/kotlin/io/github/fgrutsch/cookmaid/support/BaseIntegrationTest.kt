package io.github.fgrutsch.cookmaid.support

import io.github.fgrutsch.cookmaid.module
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.AfterEach
import org.koin.core.context.stopKoin

abstract class BaseIntegrationTest {

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    fun integrationTest(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        environment {
            config = testConfig
        }
        application {
            module()
        }
        block()
    }

    fun ApplicationTestBuilder.jsonClient(): HttpClient = createClient {
        install(ContentNegotiation) { json() }
    }
}
