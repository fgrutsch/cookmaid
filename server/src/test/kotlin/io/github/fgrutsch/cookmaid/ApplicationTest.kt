package io.github.fgrutsch.cookmaid

import io.github.fgrutsch.cookmaid.support.BaseIntegrationTest
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ApplicationTest : BaseIntegrationTest() {

    @Test
    fun testRoot() = integrationTest {
        val response = client.get("/")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
