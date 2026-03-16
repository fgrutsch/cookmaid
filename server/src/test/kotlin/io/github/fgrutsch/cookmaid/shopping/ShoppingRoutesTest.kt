package io.github.fgrutsch.cookmaid.shopping

import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.support.BaseIntegrationTest
import io.github.fgrutsch.cookmaid.support.TestJwt
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ShoppingRoutesTest : BaseIntegrationTest() {

    @Test
    fun `GET shopping-lists returns 401 without token`() = integrationTest {
        val response = client.get("/api/shopping-lists")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `full shopping list lifecycle`() = integrationTest {
        val token = TestJwt.generateToken("shopping-test-user")
        val client = jsonClient()

        // Register user (auto-creates default shopping list)
        client.post("/api/users/me") { bearerAuth(token) }

        // Verify default list was created
        val initialLists = client.get("/api/shopping-lists") {
            bearerAuth(token)
        }.body<List<ShoppingList>>()
        assertEquals(1, initialLists.size)
        assertEquals("Shopping List", initialLists.first().name)
        assertTrue(initialLists.first().default)
        val defaultList = initialLists.first()

        // Create an additional list
        val createResponse = client.post("/api/shopping-lists") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateListRequest("Groceries"))
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createdList = createResponse.body<ShoppingList>()
        assertEquals("Groceries", createdList.name)

        // Verify both lists exist
        val lists = client.get("/api/shopping-lists") {
            bearerAuth(token)
        }.body<List<ShoppingList>>()
        assertEquals(2, lists.size)

        // Add a free-text item to the default list
        val addItemResponse = client.post("/api/shopping-lists/${defaultList.id}/items") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateShoppingItemRequest(freeTextName = "Paper towels", quantity = 3f))
        }
        assertEquals(HttpStatusCode.Created, addItemResponse.status)
        val addedItem = addItemResponse.body<ShoppingItem>()
        assertNotNull(addedItem.id)

        // Add a catalog item (fetch one via API first)
        val catalogItems = client.get("/api/catalog-items") {
            bearerAuth(token)
        }.body<List<Item.CatalogItem>>()
        val catalogItem = catalogItems.first()
        val addCatalogResponse = client.post("/api/shopping-lists/${defaultList.id}/items") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateShoppingItemRequest(catalogItemId = catalogItem.id, quantity = 2f))
        }
        assertEquals(HttpStatusCode.Created, addCatalogResponse.status)
        val addedCatalogItem = addCatalogResponse.body<ShoppingItem>()
        val returnedItem = addedCatalogItem.item as Item.CatalogItem
        assertEquals(catalogItem.name, returnedItem.name)
        assertEquals(catalogItem.category.name, returnedItem.category.name)

        // Get items for list
        val itemsResponse = client.get("/api/shopping-lists/${defaultList.id}/items") {
            bearerAuth(token)
        }
        assertEquals(HttpStatusCode.OK, itemsResponse.status)
        val items = itemsResponse.body<List<ShoppingItem>>()
        assertEquals(2, items.size)

        // Update item (mark checked)
        val updateItemResponse = client.put("/api/shopping-lists/${defaultList.id}/items/${addedItem.id}") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(UpdateItemRequest(quantity = 3f, checked = true))
        }
        assertEquals(HttpStatusCode.NoContent, updateItemResponse.status)

        // Verify item is checked
        val itemsAfterUpdate = client.get("/api/shopping-lists/${defaultList.id}/items") {
            bearerAuth(token)
        }.body<List<ShoppingItem>>()
        assertTrue(itemsAfterUpdate.any { it.id == addedItem.id && it.checked })

        // Delete checked items
        val deleteCheckedResponse = client.delete("/api/shopping-lists/${defaultList.id}/items?checked=true") {
            bearerAuth(token)
        }
        assertEquals(HttpStatusCode.NoContent, deleteCheckedResponse.status)

        // Verify only unchecked items remain
        val itemsAfterDeleteChecked = client.get("/api/shopping-lists/${defaultList.id}/items") {
            bearerAuth(token)
        }.body<List<ShoppingItem>>()
        assertEquals(1, itemsAfterDeleteChecked.size)

        // Update list (rename)
        val updateListResponse = client.put("/api/shopping-lists/${defaultList.id}") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(UpdateListRequest("Weekly Groceries"))
        }
        assertEquals(HttpStatusCode.NoContent, updateListResponse.status)

        // Cannot delete default list
        val deleteDefaultResponse = client.delete("/api/shopping-lists/${defaultList.id}") {
            bearerAuth(token)
        }
        assertEquals(HttpStatusCode.Conflict, deleteDefaultResponse.status)

        // Can delete non-default list
        val deleteResponse = client.delete("/api/shopping-lists/${createdList.id}") {
            bearerAuth(token)
        }
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Default list still exists
        val finalLists = client.get("/api/shopping-lists") {
            bearerAuth(token)
        }.body<List<ShoppingList>>()
        assertEquals(1, finalLists.size)
        assertTrue(finalLists.first().default)
    }
}
