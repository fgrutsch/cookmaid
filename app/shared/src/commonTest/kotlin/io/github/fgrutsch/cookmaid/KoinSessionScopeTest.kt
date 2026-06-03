package io.github.fgrutsch.cookmaid

import io.github.fgrutsch.cookmaid.ui.auth.authModule
import io.github.fgrutsch.cookmaid.ui.catalog.catalogModule
import io.github.fgrutsch.cookmaid.ui.mealplan.mealPlanModule
import io.github.fgrutsch.cookmaid.ui.recipe.recipeModule
import io.github.fgrutsch.cookmaid.ui.settings.settingsModule
import io.github.fgrutsch.cookmaid.ui.shopping.shoppingModule
import io.github.fgrutsch.cookmaid.ui.user.userModule
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

class KoinSessionScopeTest {

    @Test
    fun `unloadModules + loadModules produces fresh singleton instances`() {
        val testModule = module { singleOf(::SessionMarker) }
        val app = koinApplication { modules(testModule) }
        val koin = app.koin

        val first = koin.get<SessionMarker>()
        first.data = "user-a"

        koin.unloadModules(listOf(testModule))
        koin.loadModules(listOf(testModule))

        val second = koin.get<SessionMarker>()

        assertNotSame(first, second)
        assertEquals("clean", second.data)

        app.close()
    }

    @Test
    fun `appModules and sessionModules together cover all feature modules`() {
        val all = appModules + sessionModules
        val expected = listOf(
            authModule,
            userModule,
            settingsModule,
            catalogModule,
            shoppingModule,
            recipeModule,
            mealPlanModule,
        )
        assertEquals(expected.toSet(), all.toSet())
    }

    @Test
    fun `sessionModules contains only user-scoped feature modules`() {
        assertEquals(
            setOf(catalogModule, shoppingModule, recipeModule, mealPlanModule),
            sessionModules.toSet(),
        )
    }

    @Test
    fun `appModules contains only auth, user, and settings modules`() {
        assertEquals(
            setOf(authModule, userModule, settingsModule),
            appModules.toSet(),
        )
    }
}

private class SessionMarker {
    var data: String = "clean"
}
