package io.github.fgrutsch.cookmaid.catalog

import io.github.fgrutsch.cookmaid.support.BaseIntegrationTest
import io.github.fgrutsch.cookmaid.support.TestJwt
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogItemRoutesTest : BaseIntegrationTest() {

    @Test
    fun `GET catalog-items returns 401 without token`() = integrationTest {
        val response = client.get("/api/catalog-items")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET catalog-items returns all items with categories`() = integrationTest {
        val token = TestJwt.generateToken("test-subject")

        val response = jsonClient().get("/api/catalog-items") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val items = response.body<List<Item.Catalog>>()
        assertTrue(items.size > 100)
        assertTrue(items.all { it.category.name.isNotBlank() })
    }
}
