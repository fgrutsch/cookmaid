package io.github.fgrutsch.cookmaid.ui.auth

import io.github.fgrutsch.cookmaid.ui.catalog.ApiCatalogItemRepository
import io.github.fgrutsch.cookmaid.ui.mealplan.ApiMealPlanRepository
import io.github.fgrutsch.cookmaid.ui.mealplan.MealPlanViewModel
import io.github.fgrutsch.cookmaid.ui.recipe.ApiRecipeRepository
import io.github.fgrutsch.cookmaid.ui.recipe.list.RecipeListViewModel
import io.github.fgrutsch.cookmaid.ui.shopping.ApiShoppingListRepository
import io.github.fgrutsch.cookmaid.ui.shopping.ShoppingListViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import org.publicvalue.multiplatform.oidc.ktor.clearTokens
import org.publicvalue.multiplatform.oidc.tokenstore.TokenStore
import org.publicvalue.multiplatform.oidc.tokenstore.removeTokens

/**
 * Owns the full logout-time cleanup sequence: OIDC tokens, Ktor auth state,
 * repository caches, and singleton ViewModel state. Every user-scoped
 * singleton registered here is dropped when [clearAll] runs.
 *
 * **Register new user-scoped singletons here.** This is the single
 * grep-able place for "what dies when a user logs out". Forgetting to
 * register a new singleton will reintroduce the cross-user leak that
 * this class exists to prevent.
 *
 * Each step runs in its own try/catch so a single failure does not abort
 * the rest of the sequence. Token removal runs first — if anything else
 * fails the tokens are still gone, so subsequent requests 401.
 *
 * Intentionally a concrete class, not an interface — there is exactly
 * one implementation and one consumer ([OidcAuthHandler]). Tests that
 * want to skip cleanup can inject a no-op subclass.
 */
@Suppress("LongParameterList") // inherent to the aggregator pattern: the cleaner owns every user-scoped singleton
class SessionCleaner(
    private val tokenStore: TokenStore,
    private val httpClient: HttpClient,
    private val shoppingListRepository: ApiShoppingListRepository,
    private val catalogItemRepository: ApiCatalogItemRepository,
    private val mealPlanRepository: ApiMealPlanRepository,
    private val recipeRepository: ApiRecipeRepository,
    private val recipeListViewModel: RecipeListViewModel,
    private val shoppingListViewModel: ShoppingListViewModel,
    private val mealPlanViewModel: MealPlanViewModel,
) {

    /**
     * Runs every cleanup step best-effort. Never throws.
     *
     * Ordering is deliberate: tokens and Ktor auth are cleared first so
     * any in-flight request that races with logout receives 401 rather
     * than succeeding and repopulating a just-cleared cache. Repositories
     * and ViewModels clear afterwards — in any order, since they don't
     * depend on each other.
     */
    suspend fun clearAll() {
        runStep { tokenStore.removeTokens() }
        runStep { httpClient.clearTokens() }
        runStep { shoppingListRepository.clear() }
        runStep { catalogItemRepository.clear() }
        runStep { mealPlanRepository.clear() }
        runStep { recipeRepository.clear() }
        runStep { recipeListViewModel.resetState() }
        runStep { shoppingListViewModel.resetState() }
        runStep { mealPlanViewModel.resetState() }
    }

    private suspend fun runStep(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            // Honor structured concurrency — never swallow cancellation.
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception
        ) {
            // No central logger yet; surface the failure for debug builds.
            // See #73 for rationale — best-effort cleanup must stay visible.
            @Suppress("PrintStackTrace")
            e.printStackTrace()
        }
    }
}
