package io.github.fgrutsch.cookmaid

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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import io.github.fgrutsch.cookmaid.navigation.Route
import io.github.fgrutsch.cookmaid.navigation.TopLevelRoute
import io.github.fgrutsch.cookmaid.navigation.navConfig
import io.github.fgrutsch.cookmaid.ui.auth.AuthEvent
import io.github.fgrutsch.cookmaid.ui.auth.AuthState
import io.github.fgrutsch.cookmaid.ui.auth.AuthViewModel
import io.github.fgrutsch.cookmaid.ui.auth.LoginScreen
import io.github.fgrutsch.cookmaid.ui.auth.OidcConfig
import io.github.fgrutsch.cookmaid.ui.auth.UserProfile
import io.github.fgrutsch.cookmaid.ui.mealplan.MealPlanScreen
import io.github.fgrutsch.cookmaid.ui.mealplan.MealPlanViewModel
import io.github.fgrutsch.cookmaid.ui.recipe.detail.RecipeDetailScreen
import io.github.fgrutsch.cookmaid.ui.recipe.detail.RecipeDetailViewModel
import io.github.fgrutsch.cookmaid.ui.recipe.edit.AddRecipeScreen
import io.github.fgrutsch.cookmaid.ui.recipe.edit.AddRecipeViewModel
import io.github.fgrutsch.cookmaid.ui.recipe.list.RecipeListScreen
import io.github.fgrutsch.cookmaid.ui.recipe.list.RecipeListViewModel
import io.github.fgrutsch.cookmaid.ui.settings.SettingsScreen
import io.github.fgrutsch.cookmaid.ui.settings.SettingsViewModel
import io.github.fgrutsch.cookmaid.ui.shopping.ShoppingListScreen
import io.github.fgrutsch.cookmaid.ui.shopping.ShoppingListViewModel
import io.github.fgrutsch.cookmaid.ui.theme.AppTheme
import org.koin.compose.KoinApplication
import org.koin.compose.getKoin
import io.github.fgrutsch.cookmaid.ui.common.LocalAppLocale
import io.github.fgrutsch.cookmaid.ui.common.resolve
import org.koin.compose.koinInject
import org.koin.dsl.module
import org.publicvalue.multiplatform.oidc.flows.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.tokenstore.TokenStore

/**
 * Root composable that sets up Koin DI, authentication, and navigation.
 *
 * @param apiBaseUrl the base URL for the backend API.
 * @param oidcConfig the OpenID Connect configuration.
 * @param codeAuthFlowFactory factory for the OIDC authorization code flow.
 */
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
        val settingsState by settingsViewModel.state.collectAsState()

        CompositionLocalProvider(
            LocalAppLocale provides (settingsState.locale?.code),
        ) {
            key(settingsState.locale) {
                AppTheme(isDark = settingsState.isDarkMode) {
                    val authViewModel = koinInject<AuthViewModel>()
                    val authState by authViewModel.state.collectAsState()

                    LaunchedEffect(Unit) {
                        authViewModel.onEvent(AuthEvent.Initialize)
                    }

                    when (authState.status) {
                        AuthState.Status.Initializing -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }

                        AuthState.Status.Unauthenticated -> LoginScreen(viewModel = authViewModel)
                        AuthState.Status.Authenticated -> MainContent(
                            settingsViewModel = settingsViewModel,
                            authViewModel = authViewModel,
                            userProfile = authState.profile,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainContent(
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    userProfile: UserProfile,
) {
    val backStack = rememberNavBackStack(navConfig, Route.ShoppingList)
    var selectedTab by remember { mutableStateOf(TopLevelRoute.Shopping) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    if (selectedTab != tab) {
                        selectedTab = tab
                        backStack.clear()
                        backStack.add(tab.startRoute)
                    }
                },
            )
        },
    ) { innerPadding ->
        AppNavDisplay(
            backStack = backStack,
            settingsViewModel = settingsViewModel,
            authViewModel = authViewModel,
            userProfile = userProfile,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun BottomNavigationBar(
    selectedTab: TopLevelRoute,
    onTabSelected: (TopLevelRoute) -> Unit,
) {
    NavigationBar {
        TopLevelRoute.entries.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(tab.icon, contentDescription = tab.labelRes.resolve())
                },
                label = { Text(tab.labelRes.resolve()) },
            )
        }
    }
}

@Composable
private fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    userProfile: UserProfile,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
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
                val koin = getKoin()
                AddRecipeScreen(
                    viewModel = remember {
                        AddRecipeViewModel(koin.get(), koin.get())
                    },
                    onBack = { backStack.removeLastOrNull() },
                )
            }

            entry<Route.RecipeDetail> { key ->
                val koin = getKoin()
                RecipeDetailScreen(
                    viewModel = remember(key.id) {
                        RecipeDetailViewModel(key.id, koin.get(), koin.get(), koin.get())
                    },
                    onBack = { backStack.removeLastOrNull() },
                    onEdit = { backStack.add(Route.EditRecipe(key.id)) },
                )
            }

            entry<Route.EditRecipe> { key ->
                val koin = getKoin()
                AddRecipeScreen(
                    viewModel = remember(key.id) {
                        AddRecipeViewModel(koin.get(), koin.get(), key.id)
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
                    onLogout = { authViewModel.onEvent(AuthEvent.Logout) },
                )
            }
        },
    )
}
