package io.github.fgrutsch.cookmaid.support

import io.github.fgrutsch.cookmaid.catalog.catalogModule
import io.github.fgrutsch.cookmaid.db.databaseModule
import io.github.fgrutsch.cookmaid.mealplan.MealPlanItemsTable
import io.github.fgrutsch.cookmaid.mealplan.mealPlanModule
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredientsTable
import io.github.fgrutsch.cookmaid.recipe.RecipesTable
import io.github.fgrutsch.cookmaid.recipe.recipeModule
import io.github.fgrutsch.cookmaid.shopping.ShoppingItemsTable
import io.github.fgrutsch.cookmaid.shopping.ShoppingListsTable
import io.github.fgrutsch.cookmaid.shopping.shoppingModule
import io.github.fgrutsch.cookmaid.user.UsersTable
import io.github.fgrutsch.cookmaid.user.userModule
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
            recipeModule,
            mealPlanModule,
        )
    }

    @BeforeEach
    fun cleanDatabase() {
        val db = getKoin().get<Database>()
        transaction(db) {
            MealPlanItemsTable.deleteAll()
            RecipeIngredientsTable.deleteAll()
            RecipesTable.deleteAll()
            ShoppingItemsTable.deleteAll()
            ShoppingListsTable.deleteAll()
            UsersTable.deleteAll()
        }
    }
}
