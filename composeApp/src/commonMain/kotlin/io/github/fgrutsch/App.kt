package io.github.fgrutsch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import io.github.fgrutsch.navigation.Route
import io.github.fgrutsch.navigation.TopLevelRoute
import io.github.fgrutsch.navigation.navConfig
import io.github.fgrutsch.ui.auth.AuthState
import io.github.fgrutsch.ui.auth.AuthViewModel
import io.github.fgrutsch.ui.auth.LoginScreen
import io.github.fgrutsch.ui.auth.OidcConfig
import io.github.fgrutsch.ui.auth.UserProfile
import io.github.fgrutsch.ui.mealplan.MealPlanScreen
import io.github.fgrutsch.ui.mealplan.MealPlanViewModel
import io.github.fgrutsch.ui.recipe.AddRecipeScreen
import io.github.fgrutsch.ui.recipe.AddRecipeViewModel
import io.github.fgrutsch.ui.recipe.RecipeDetailScreen
import io.github.fgrutsch.ui.recipe.RecipeDetailViewModel
import io.github.fgrutsch.ui.recipe.RecipeListScreen
import io.github.fgrutsch.ui.recipe.RecipeListViewModel
import io.github.fgrutsch.ui.settings.SettingsScreen
import io.github.fgrutsch.ui.settings.SettingsViewModel
import io.github.fgrutsch.ui.shopping.ShoppingListScreen
import io.github.fgrutsch.ui.shopping.ShoppingListViewModel
import io.github.fgrutsch.ui.theme.AppTheme
import org.koin.compose.KoinApplication
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.dsl.module
import org.publicvalue.multiplatform.oidc.flows.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.tokenstore.TokenStore

@Composable
fun App(
    apiBaseUrl: ApiBaseUrl,
    oidcConfig: OidcConfig,
    codeAuthFlowFactory: CodeAuthFlowFactory,
    tokenStore: TokenStore,
) {
    val platformModule = module {
        single { apiBaseUrl }
        single { oidcConfig }
        single<CodeAuthFlowFactory> { codeAuthFlowFactory }
        single<TokenStore> { tokenStore }
    }

    KoinApplication(application = {
        modules(allModules + platformModule)
    }) {
        val settingsViewModel = koinInject<SettingsViewModel>()
        val isDarkMode by settingsViewModel.isDarkMode.collectAsState()

        AppTheme(isDark = isDarkMode) {
            val authViewModel = koinInject<AuthViewModel>()
            val authState by authViewModel.state.collectAsState()

            when (val auth = authState) {
                is AuthState.Initializing -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AuthState.Unauthenticated -> LoginScreen(viewModel = authViewModel)
                is AuthState.Authenticated -> MainContent(
                    settingsViewModel = settingsViewModel,
                    authViewModel = authViewModel,
                    userProfile = auth.profile,
                )
            }
        }
    }
}

@Composable
private fun MainContent(settingsViewModel: SettingsViewModel, authViewModel: AuthViewModel, userProfile: UserProfile) {
    val backStack = rememberNavBackStack(navConfig, Route.ShoppingList)
    var selectedTab by remember { mutableStateOf(TopLevelRoute.Shopping) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelRoute.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            if (selectedTab != tab) {
                                selectedTab = tab
                                backStack.clear()
                                backStack.add(tab.startRoute)
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.padding(innerPadding),
            entryProvider = entryProvider {
                entry<Route.ShoppingList> {
                    ShoppingListScreen(viewModel = koinInject<ShoppingListViewModel>())
                }

                entry<Route.RecipeList> {
                    RecipeListScreen(
                        viewModel = koinInject<RecipeListViewModel>(),
                        onRecipeClick = { id -> backStack.add(Route.RecipeDetail(id)) },
                        onAddRecipe = { backStack.add(Route.AddRecipe) },
                    )
                }

                entry<Route.AddRecipe> {
                    AddRecipeScreen(
                        viewModel = koinInject<AddRecipeViewModel>(),
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<Route.RecipeDetail> { key ->
                    val koin = getKoin()
                    RecipeDetailScreen(
                        viewModel = remember(key.id) {
                            RecipeDetailViewModel(
                                recipeId = key.id,
                                recipeRepository = koin.get(),
                                shoppingListRepository = koin.get(),
                                mealPlanRepository = koin.get(),
                            )
                        },
                        onBack = { backStack.removeLastOrNull() },
                        onEdit = { backStack.add(Route.EditRecipe(key.id)) },
                    )
                }

                entry<Route.EditRecipe> { key ->
                    val koin = getKoin()
                    AddRecipeScreen(
                        viewModel = remember(key.id) {
                            AddRecipeViewModel(
                                recipeRepository = koin.get(),
                                tagRepository = koin.get(),
                                catalogItemRepository = koin.get(),
                                editRecipeId = key.id,
                            )
                        },
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<Route.MealPlan> {
                    MealPlanScreen(
                        viewModel = koinInject<MealPlanViewModel>(),
                        onRecipeClick = { id -> backStack.add(Route.RecipeDetail(id)) },
                    )
                }

                entry<Route.Settings> {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        userProfile = userProfile,
                        onLogout = { authViewModel.logout() },
                    )
                }
            },
        )
    }
}
