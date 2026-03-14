package io.github.fgrutsch.support

import io.github.fgrutsch.catalog.catalogModule
import io.github.fgrutsch.db.databaseModule
import io.github.fgrutsch.shopping.ShoppingItemsTable
import io.github.fgrutsch.shopping.ShoppingListsTable
import io.github.fgrutsch.shopping.shoppingModule
import io.github.fgrutsch.user.UsersTable
import io.github.fgrutsch.user.userModule
import io.ktor.server.config.ApplicationConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.junit5.KoinTestExtension

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseTest : KoinTest {

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        modules(
            module { single<ApplicationConfig> { testConfig } },
            databaseModule,
            userModule,
            catalogModule,
            shoppingModule,
        )
    }

    @BeforeEach
    fun cleanDatabase() {
        val db = getKoin().get<Database>()
        transaction(db) {
            ShoppingItemsTable.deleteAll()
            ShoppingListsTable.deleteAll()
            UsersTable.deleteAll()
        }
    }
}
