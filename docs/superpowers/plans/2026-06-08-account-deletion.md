# Account Deletion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let users permanently delete their Cookmaid account and all data via an in-app screen and a public web deeplink, satisfying Google Play's account-deletion requirement.

**Architecture:** A new `DELETE /api/users/me` endpoint deletes the `users` row (Postgres `ON DELETE CASCADE` removes all owned data) and evicts the user cache. A dedicated Compose `DeleteAccountScreen` (shared by Android + Web) confirms, calls the endpoint, then logs out. The web app exposes `/delete-account` as an authed deeplink: `main.kt` derives the deeplink from `window.location` and passes it into `App(startDeeplink=...)`, which navigates to it once authenticated (the wasmJS OIDC flow is a popup, so the main window keeps the URL through login — no persistence needed). The identity provider (Logto/OIDC) is **not** touched — deletion is data-only and generic across providers.

**Tech Stack:** Kotlin Multiplatform, Ktor (server), Exposed, Compose Multiplatform, Koin, kotlinx-serialization, JUnit5 + Testcontainers (server), kotlin.test + coroutines-test (shared).

**Spec:** `docs/superpowers/specs/2026-06-08-account-deletion-design.md`

**Branch:** `feat/account-deletion` (already checked out).

> Note: the spec mentions "404 if not registered" — the actual mapping in
> `Application.kt` is **401** (`UserNotRegisteredException` →
> `HttpStatusCode.Unauthorized` with body `{"error":"user_not_registered"}`).
> This plan uses 401. No code change to that mapping.

---

## Task 1: Server — `UserRepository.delete`

**Files:**
- Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/user/UserRepository.kt`
- Test: `server/src/test/kotlin/io/github/fgrutsch/cookmaid/user/PostgresUserRepositoryTest.kt`

- [ ] **Step 1: Write the failing tests**

Add these two tests to `PostgresUserRepositoryTest` (class already imports `UserId` package siblings, `assertNull`, `assertNotNull`):

```kotlin
    @Test
    fun `delete removes the user`() = runTest {
        val user = repository.create("oidc-subject-1")

        repository.delete(UserId(user.id))

        assertNull(repository.findByOidcSubject("oidc-subject-1"))
    }

    @Test
    fun `delete only removes the targeted user`() = runTest {
        val user1 = repository.create("oidc-subject-1")
        repository.create("oidc-subject-2")

        repository.delete(UserId(user1.id))

        assertNull(repository.findByOidcSubject("oidc-subject-1"))
        assertNotNull(repository.findByOidcSubject("oidc-subject-2"))
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :server:test --tests "io.github.fgrutsch.cookmaid.user.PostgresUserRepositoryTest"`
Expected: FAIL — `delete` is unresolved (compile error).

- [ ] **Step 3: Add `delete` to the interface and implementation**

In `UserRepository.kt`, add to the `interface UserRepository`:

```kotlin
    /**
     * Deletes the user with the given [userId]. All owned data
     * (recipes, meal plan items, shopping lists) is removed via FK cascade.
     *
     * @param userId the id of the user to delete.
     */
    suspend fun delete(userId: UserId)
```

Add to `class PostgresUserRepository`:

```kotlin
    override suspend fun delete(userId: UserId): Unit = suspendTransaction {
        UsersTable.deleteWhere { id eq userId.value }
    }
```

Add these imports to `UserRepository.kt`:

```kotlin
import io.github.fgrutsch.cookmaid.user.UserId
import org.jetbrains.exposed.v1.jdbc.deleteWhere
```

> `UserId` is in the same package; if the IDE flags the import as redundant, drop it. `eq` is already imported.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :server:test --tests "io.github.fgrutsch.cookmaid.user.PostgresUserRepositoryTest"`
Expected: PASS (all tests).

- [ ] **Step 5: Commit**

```bash
git add server/src/main/kotlin/io/github/fgrutsch/cookmaid/user/UserRepository.kt server/src/test/kotlin/io/github/fgrutsch/cookmaid/user/PostgresUserRepositoryTest.kt
git commit -m "feat(server): add UserRepository.delete"
```

---

## Task 2: Server — `UserService.delete` with cache eviction

**Files:**
- Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/user/UserService.kt`
- Test: `server/src/test/kotlin/io/github/fgrutsch/cookmaid/user/UserServiceTest.kt`

- [ ] **Step 1: Write the failing tests**

Add to `UserServiceTest` (already imports `ShoppingListRepository`, `UserId`, `assertNull`, `assertEquals`, `inject`):

```kotlin
    @Test
    fun `delete removes the user and evicts the cache`() = runTest {
        val user = service.getOrCreate("oidc-subject-1")
        service.findIdByOidcSubject("oidc-subject-1") // prime the cache

        service.delete(UserId(user.id), "oidc-subject-1")

        assertNull(service.findIdByOidcSubject("oidc-subject-1"))
    }

    @Test
    fun `delete cascades to the user's shopping lists`() = runTest {
        val shoppingListRepo by inject<ShoppingListRepository>()
        val user = service.getOrCreate("oidc-subject-1")
        assertEquals(1, shoppingListRepo.find(UserId(user.id)).size)

        service.delete(UserId(user.id), "oidc-subject-1")

        assertEquals(0, shoppingListRepo.find(UserId(user.id)).size)
    }

    @Test
    fun `delete does not affect other users`() = runTest {
        val user1 = service.getOrCreate("oidc-subject-1")
        val user2 = service.getOrCreate("oidc-subject-2")

        service.delete(UserId(user1.id), "oidc-subject-1")

        assertNull(service.findIdByOidcSubject("oidc-subject-1"))
        assertEquals(user2.id, service.findIdByOidcSubject("oidc-subject-2")?.value)
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :server:test --tests "io.github.fgrutsch.cookmaid.user.UserServiceTest"`
Expected: FAIL — `delete` is unresolved.

- [ ] **Step 3: Add `delete` to `UserService`**

Add this method to `class UserService`:

```kotlin
    /**
     * Deletes the user identified by [userId] and evicts the cached lookup
     * for [oidcSubject]. All owned data is removed via database cascade.
     *
     * @param userId the id of the user to delete.
     * @param oidcSubject the OIDC subject whose cache entry must be evicted.
     */
    suspend fun delete(userId: UserId, oidcSubject: String) {
        repository.delete(userId)
        cache.remove(oidcSubject)
        logger.info { "User deleted: id=${userId.value}" }
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :server:test --tests "io.github.fgrutsch.cookmaid.user.UserServiceTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add server/src/main/kotlin/io/github/fgrutsch/cookmaid/user/UserService.kt server/src/test/kotlin/io/github/fgrutsch/cookmaid/user/UserServiceTest.kt
git commit -m "feat(server): add UserService.delete with cache eviction"
```

---

## Task 3: Server — `DELETE /api/users/me` route

**Files:**
- Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/user/UserModule.kt`
- Test: `server/src/test/kotlin/io/github/fgrutsch/cookmaid/user/UserRoutesTest.kt`

- [ ] **Step 1: Write the failing tests**

Add to `UserRoutesTest`:

```kotlin
    @Test
    fun `DELETE users me returns 401 without token`() = integrationTest {
        val response = client.delete("/api/users/me")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `DELETE users me returns 401 when not registered`() = integrationTest {
        val token = TestJwt.generateToken("test-subject")

        val response = client.delete("/api/users/me") { bearerAuth(token) }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `DELETE users me deletes the registered user`() = integrationTest {
        val token = TestJwt.generateToken("test-subject")
        jsonClient().post("/api/users/me") { bearerAuth(token) } // register

        val response = client.delete("/api/users/me") { bearerAuth(token) }
        assertEquals(HttpStatusCode.NoContent, response.status)

        // user is gone: a second delete is now unauthorized (not registered)
        val second = client.delete("/api/users/me") { bearerAuth(token) }
        assertEquals(HttpStatusCode.Unauthorized, second.status)
    }
```

Add this import to `UserRoutesTest`:

```kotlin
import io.ktor.client.request.delete
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :server:test --tests "io.github.fgrutsch.cookmaid.user.UserRoutesTest"`
Expected: FAIL — `DELETE /api/users/me` returns 405/404, not the expected statuses.

- [ ] **Step 3: Add the route**

In `UserModule.kt`, add the `delete("/me")` block inside `route("/users")`, after the existing `post("/me")`:

```kotlin
        delete("/me") {
            val userId = call.userId()
            val subject = requireNotNull(call.principal<JWTPrincipal>()) {
                "JWT principal missing"
            }.payload.subject
            service.delete(userId, subject)
            call.respond(HttpStatusCode.NoContent)
        }
```

Add these imports to `UserModule.kt`:

```kotlin
import io.github.fgrutsch.cookmaid.common.ktor.userId
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.delete
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :server:test --tests "io.github.fgrutsch.cookmaid.user.UserRoutesTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add server/src/main/kotlin/io/github/fgrutsch/cookmaid/user/UserModule.kt server/src/test/kotlin/io/github/fgrutsch/cookmaid/user/UserRoutesTest.kt
git commit -m "feat(server): add DELETE /api/users/me endpoint"
```

---

## Task 4: Client — `UserClient` interface + `deleteAccount`

Converts the concrete `UserClient` to an interface (`ApiUserClient` impl) so it can be faked in tests, and adds `deleteAccount()`.

**Files:**
- Modify: `app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/user/UserClient.kt`
- Modify: `app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/user/UserModule.kt`
- Create: `app/shared/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/user/FakeUserClient.kt`

- [ ] **Step 1: Replace `UserClient.kt` with an interface + impl**

Full new contents of `UserClient.kt`:

```kotlin
package io.github.fgrutsch.cookmaid.ui.user

import io.github.fgrutsch.cookmaid.user.User
import io.github.fgrutsch.cookmaid.ui.auth.ApiClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.post

/**
 * HTTP client for user account operations.
 */
interface UserClient {
    /**
     * Registers the authenticated user if needed and returns the user record.
     */
    suspend fun getOrCreateUser(): User

    /**
     * Permanently deletes the authenticated user's account and all associated data.
     */
    suspend fun deleteAccount()
}

class ApiUserClient(
    private val apiClient: ApiClient,
) : UserClient {
    override suspend fun getOrCreateUser(): User =
        apiClient.httpClient.post("/api/users/me").body()

    override suspend fun deleteAccount() {
        apiClient.httpClient.delete("/api/users/me")
    }
}
```

- [ ] **Step 2: Update the Koin binding**

Full new contents of `UserModule.kt`:

```kotlin
package io.github.fgrutsch.cookmaid.ui.user

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val userModule = module {
    singleOf(::ApiUserClient) bind UserClient::class
}
```

> `OidcAuthHandler` takes `UserClient` (now the interface) — no change needed there.

- [ ] **Step 3: Create the test fake**

Create `FakeUserClient.kt`:

```kotlin
package io.github.fgrutsch.cookmaid.ui.user

import io.github.fgrutsch.cookmaid.user.User
import kotlin.uuid.Uuid

class FakeUserClient : UserClient {
    var deleteCalled: Boolean = false
    var failDelete: Boolean = false

    override suspend fun getOrCreateUser(): User =
        User(id = Uuid.random(), oidcSubject = "test-subject")

    override suspend fun deleteAccount() {
        if (failDelete) throw IllegalStateException("delete failed")
        deleteCalled = true
    }
}
```

- [ ] **Step 4: Verify the shared module still compiles and tests pass**

Run: `./gradlew :app:shared:testDebugUnitTest`
Expected: PASS (existing tests unaffected; conversion compiles).

- [ ] **Step 5: Commit**

```bash
git add app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/user/UserClient.kt app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/user/UserModule.kt app/shared/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/user/FakeUserClient.kt
git commit -m "feat(app): make UserClient an interface and add deleteAccount"
```

---

## Task 5: Client — `Route.DeleteAccount` navigation key

**Files:**
- Modify: `app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/navigation/Route.kt`
- Modify: `app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/navigation/NavConfig.kt`

- [ ] **Step 1: Add the route**

In `Route.kt`, add inside `sealed interface Route` (next to the other `data object`s):

```kotlin
    @Serializable
    data object DeleteAccount : Route
```

- [ ] **Step 2: Register it in `navConfig`**

In `NavConfig.kt`, add inside the `polymorphic(NavKey::class) { ... }` block:

```kotlin
            subclass(Route.DeleteAccount::class, Route.DeleteAccount.serializer())
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:shared:compileDebugKotlinAndroid`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/navigation/Route.kt app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/navigation/NavConfig.kt
git commit -m "feat(app): add DeleteAccount navigation route"
```

---

## Task 6: Client — `DeleteAccountViewModel` (MVI)

**Files:**
- Create: `app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/deleteaccount/DeleteAccountContract.kt`
- Create: `app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/deleteaccount/DeleteAccountViewModel.kt`
- Test: `app/shared/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/deleteaccount/DeleteAccountViewModelTest.kt`

- [ ] **Step 1: Create the contract**

Create `DeleteAccountContract.kt`:

```kotlin
package io.github.fgrutsch.cookmaid.ui.deleteaccount

/**
 * @param deleting true while the delete request is in flight.
 * @param deleted true once the account has been deleted.
 * @param error true if the delete request failed.
 */
data class DeleteAccountState(
    val deleting: Boolean = false,
    val deleted: Boolean = false,
    val error: Boolean = false,
)

sealed interface DeleteAccountEvent {
    data object Confirm : DeleteAccountEvent
}
```

- [ ] **Step 2: Write the failing test**

Create `DeleteAccountViewModelTest.kt`:

```kotlin
package io.github.fgrutsch.cookmaid.ui.deleteaccount

import io.github.fgrutsch.cookmaid.support.BaseViewModelTest
import io.github.fgrutsch.cookmaid.ui.user.FakeUserClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteAccountViewModelTest : BaseViewModelTest() {

    @Test
    fun `confirm deletes the account and sets deleted`() = viewModelTest {
        val client = FakeUserClient()
        val viewModel = DeleteAccountViewModel(client)

        viewModel.onEvent(DeleteAccountEvent.Confirm)
        advanceUntilIdle()

        assertTrue(client.deleteCalled)
        assertTrue(viewModel.state.value.deleted)
        assertFalse(viewModel.state.value.deleting)
        assertFalse(viewModel.state.value.error)
    }

    @Test
    fun `confirm failure sets error and leaves account not deleted`() = viewModelTest {
        val client = FakeUserClient().apply { failDelete = true }
        val viewModel = DeleteAccountViewModel(client)

        viewModel.onEvent(DeleteAccountEvent.Confirm)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.deleted)
        assertTrue(viewModel.state.value.error)
        assertFalse(viewModel.state.value.deleting)
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :app:shared:testDebugUnitTest --tests "*DeleteAccountViewModelTest*"`
Expected: FAIL — `DeleteAccountViewModel` is unresolved.

- [ ] **Step 4: Create the ViewModel**

Create `DeleteAccountViewModel.kt`:

```kotlin
package io.github.fgrutsch.cookmaid.ui.deleteaccount

import io.github.fgrutsch.cookmaid.ui.common.MviViewModel
import io.github.fgrutsch.cookmaid.ui.user.UserClient

/**
 * Drives the account deletion flow: calls the delete endpoint and tracks progress.
 *
 * @property userClient the client used to delete the account.
 */
class DeleteAccountViewModel(
    private val userClient: UserClient,
) : MviViewModel<DeleteAccountState, DeleteAccountEvent, Nothing>(DeleteAccountState()) {

    override fun handleEvent(event: DeleteAccountEvent) {
        when (event) {
            DeleteAccountEvent.Confirm -> confirm()
        }
    }

    private fun confirm() {
        if (state.value.deleting || state.value.deleted) return
        updateState { copy(deleting = true, error = false) }
        launch {
            userClient.deleteAccount()
            updateState { copy(deleting = false, deleted = true) }
        }
    }

    override fun onError(e: Exception) {
        updateState { copy(deleting = false, error = true) }
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :app:shared:testDebugUnitTest --tests "*DeleteAccountViewModelTest*"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/deleteaccount/ app/shared/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/deleteaccount/
git commit -m "feat(app): add DeleteAccountViewModel"
```

---

## Task 7: Client — string resources

**Files:**
- Modify: `app/shared/src/commonMain/composeResources/values/strings.xml`
- Modify: `app/shared/src/commonMain/composeResources/values-de/strings.xml`

- [ ] **Step 1: Add English strings**

In `values/strings.xml`, add to the `<!-- Settings -->` block:

```xml
    <string name="settings_delete_account">Delete account</string>
```

And add a new block before `</resources>`:

```xml
    <!-- Delete Account -->
    <string name="delete_account_title">Delete account</string>
    <string name="delete_account_warning">This permanently deletes your Cookmaid account and all your data — recipes, meal plans and shopping lists. This cannot be undone.</string>
    <string name="delete_account_button">Delete my account</string>
    <string name="delete_account_confirm_title">Delete account?</string>
    <string name="delete_account_confirm_message">This cannot be undone.</string>
    <string name="delete_account_confirm">Delete</string>
    <string name="delete_account_error">Couldn't delete your account. Please try again.</string>
    <string name="delete_account_deleted_title">Account deleted</string>
    <string name="delete_account_deleted_message">Your account and all associated data have been deleted.</string>
    <string name="delete_account_finish">Finish</string>
```

- [ ] **Step 2: Add German strings**

In `values-de/strings.xml`, add to the `<!-- Settings -->` block:

```xml
    <string name="settings_delete_account">Konto löschen</string>
```

And add a new block before `</resources>`:

```xml
    <!-- Delete Account -->
    <string name="delete_account_title">Konto löschen</string>
    <string name="delete_account_warning">Damit werden dein Cookmaid-Konto und alle deine Daten – Rezepte, Essenspläne und Einkaufslisten – dauerhaft gelöscht. Dies kann nicht rückgängig gemacht werden.</string>
    <string name="delete_account_button">Mein Konto löschen</string>
    <string name="delete_account_confirm_title">Konto löschen?</string>
    <string name="delete_account_confirm_message">Dies kann nicht rückgängig gemacht werden.</string>
    <string name="delete_account_confirm">Löschen</string>
    <string name="delete_account_error">Dein Konto konnte nicht gelöscht werden. Bitte versuche es erneut.</string>
    <string name="delete_account_deleted_title">Konto gelöscht</string>
    <string name="delete_account_deleted_message">Dein Konto und alle zugehörigen Daten wurden gelöscht.</string>
    <string name="delete_account_finish">Fertig</string>
```

- [ ] **Step 3: Verify resources generate**

Run: `./gradlew :app:shared:generateComposeResClass`
Expected: PASS (new `Res.string.delete_account_*` accessors generated).

- [ ] **Step 4: Commit**

```bash
git add app/shared/src/commonMain/composeResources/values/strings.xml app/shared/src/commonMain/composeResources/values-de/strings.xml
git commit -m "feat(app): add account deletion strings (en, de)"
```

---

## Task 8: Client — `DeleteAccountScreen`

**Files:**
- Create: `app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/deleteaccount/DeleteAccountScreen.kt`

- [ ] **Step 1: Create the screen**

Create `DeleteAccountScreen.kt`:

```kotlin
package io.github.fgrutsch.cookmaid.ui.deleteaccount

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cookmaid.app.shared.generated.resources.Res
import cookmaid.app.shared.generated.resources.common_cancel
import cookmaid.app.shared.generated.resources.common_back
import cookmaid.app.shared.generated.resources.delete_account_button
import cookmaid.app.shared.generated.resources.delete_account_confirm
import cookmaid.app.shared.generated.resources.delete_account_confirm_message
import cookmaid.app.shared.generated.resources.delete_account_confirm_title
import cookmaid.app.shared.generated.resources.delete_account_deleted_message
import cookmaid.app.shared.generated.resources.delete_account_deleted_title
import cookmaid.app.shared.generated.resources.delete_account_error
import cookmaid.app.shared.generated.resources.delete_account_finish
import cookmaid.app.shared.generated.resources.delete_account_title
import cookmaid.app.shared.generated.resources.delete_account_warning
import cookmaid.app.shared.generated.resources.ic_arrow_back
import io.github.fgrutsch.cookmaid.ui.common.resolve
import org.jetbrains.compose.resources.painterResource

/**
 * Screen that lets the authenticated user permanently delete their account.
 * Shows a warning + confirm dialog; on success shows a confirmation and a
 * Finish action that triggers logout via [onFinish].
 *
 * @param viewModel the account-deletion view model.
 * @param onBack called when the user navigates back without deleting.
 * @param onFinish called after deletion completes (the caller logs the user out).
 */
@Composable
fun DeleteAccountScreen(
    viewModel: DeleteAccountViewModel,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var showConfirm by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(Res.string.delete_account_title.resolve()) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                navigationIcon = {
                    if (!state.deleted) {
                        IconButton(onClick = onBack) {
                            Icon(
                                painterResource(Res.drawable.ic_arrow_back),
                                contentDescription = Res.string.common_back.resolve(),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (state.deleted) {
            DeletedContent(
                onFinish = onFinish,
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            )
        } else {
            DeleteContent(
                deleting = state.deleting,
                error = state.error,
                onDeleteClick = { showConfirm = true },
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            )
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(Res.string.delete_account_confirm_title.resolve()) },
            text = { Text(Res.string.delete_account_confirm_message.resolve()) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        viewModel.onEvent(DeleteAccountEvent.Confirm)
                    },
                ) {
                    Text(Res.string.delete_account_confirm.resolve())
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text(Res.string.common_cancel.resolve())
                }
            },
        )
    }
}

@Composable
private fun DeleteContent(
    deleting: Boolean,
    error: Boolean,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = Res.string.delete_account_warning.resolve(),
            style = MaterialTheme.typography.bodyLarge,
        )

        if (error) {
            Text(
                text = Res.string.delete_account_error.resolve(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Button(
            onClick = onDeleteClick,
            enabled = !deleting,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            if (deleting) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    color = MaterialTheme.colorScheme.onError,
                )
            } else {
                Text(Res.string.delete_account_button.resolve())
            }
        }
    }
}

@Composable
private fun DeletedContent(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = Res.string.delete_account_deleted_title.resolve(),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = Res.string.delete_account_deleted_message.resolve(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(Res.string.delete_account_finish.resolve())
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:shared:compileDebugKotlinAndroid`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/deleteaccount/DeleteAccountScreen.kt
git commit -m "feat(app): add DeleteAccountScreen"
```

---

## Task 9: Client — wire navigation entry + Settings entry

**Files:**
- Modify: `app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/settings/SettingsScreen.kt`
- Modify: `app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/App.kt`

- [ ] **Step 1: Add a Delete Account button to Settings**

In `SettingsScreen.kt`, add a parameter `onDeleteAccount: () -> Unit` to **both** `SettingsScreen(...)` and `SettingsContent(...)`, pass it through, and render a destructive text button.

Update the `SettingsScreen` signature and its call to `SettingsContent`:

```kotlin
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    userProfile: UserProfile,
    accountUri: String,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
```

In the `SettingsContent(...)` call inside `SettingsScreen`, add:

```kotlin
            onDeleteAccount = onDeleteAccount,
```

Update the `SettingsContent` signature:

```kotlin
@Composable
private fun SettingsContent(
    userProfile: UserProfile,
    accountUri: String,
    darkMode: Boolean?,
    onDarkModeSelected: (Boolean?) -> Unit,
    locale: SupportedLocale?,
    onLocaleSelected: (SupportedLocale?) -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    appVersion: String,
    modifier: Modifier = Modifier,
) {
```

Inside `SettingsContent`'s `Column`, immediately after the existing sign-out `OutlinedButton { ... }` block, add:

```kotlin
        TextButton(
            onClick = onDeleteAccount,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = Res.string.settings_delete_account.resolve(),
                color = MaterialTheme.colorScheme.error,
            )
        }
```

Add this import to `SettingsScreen.kt`:

```kotlin
import cookmaid.app.shared.generated.resources.settings_delete_account
```

> `TextButton`, `Text`, `MaterialTheme`, `Modifier.fillMaxWidth`, and `resolve` are already imported in this file.

- [ ] **Step 2: Wire the nav entry and the Settings entry in `App.kt`**

In `App.kt`, in the `entry<Route.Settings>` block inside `AppNavDisplay`, add the `onDeleteAccount` callback:

```kotlin
            entry<Route.Settings> {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    userProfile = userProfile,
                    accountUri = accountUri,
                    onLogout = { authViewModel.onEvent(AuthEvent.Logout) },
                    onDeleteAccount = { backStack.add(Route.DeleteAccount) },
                )
            }
```

Then add a new entry after it, still inside the `entryProvider { ... }`:

```kotlin
            entry<Route.DeleteAccount> {
                val koin = getKoin()
                DeleteAccountScreen(
                    viewModel = remember { DeleteAccountViewModel(koin.get()) },
                    onBack = { backStack.removeLastOrNull() },
                    onFinish = { authViewModel.onEvent(AuthEvent.Logout) },
                )
            }
```

Add these imports to `App.kt`:

```kotlin
import io.github.fgrutsch.cookmaid.ui.deleteaccount.DeleteAccountScreen
import io.github.fgrutsch.cookmaid.ui.deleteaccount.DeleteAccountViewModel
```

> `getKoin`, `remember`, `Route`, `AuthEvent` are already imported. `koin.get()` resolves `UserClient` from `appModules`.

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:shared:compileDebugKotlinAndroid`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/settings/SettingsScreen.kt app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/App.kt
git commit -m "feat(app): reach DeleteAccountScreen from Settings and nav"
```

---

## Task 10: Web — `/delete-account` deeplink with login survival

> **Superseded:** this task was first built with a localStorage stash (the
> code blocks below), then refactored to pass the deeplink via an
> `App(startDeeplink=...)` parameter — the wasmJS OIDC login flow is a popup,
> so the main window keeps the URL through login and no persistence is needed.
> See the spec's "Web deeplink + login survival" section for the final design.
> The blocks below are kept for history.

A web visitor opening `https://<host>/delete-account` is routed to the Delete Account screen after authentication. The path is stashed in `localStorage` before any OIDC redirect and consumed once authenticated. Reading uses the cross-platform `Settings` (localStorage on web, no-op on Android).

**Files:**
- Create: `app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/navigation/Deeplink.kt`
- Modify: `app/webApp/src/wasmJsMain/kotlin/io/github/fgrutsch/cookmaid/main.kt`
- Modify: `app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/App.kt`

- [ ] **Step 1: Create the shared deeplink constants**

Create `Deeplink.kt`:

```kotlin
package io.github.fgrutsch.cookmaid.navigation

/**
 * Constants for the web deeplink hand-off. The web entry point stashes a
 * pending deeplink in storage before authentication; [App] consumes it once
 * the user is authenticated. Keyed via the cross-platform Settings store
 * (localStorage on web, SharedPreferences on Android).
 */
object Deeplink {
    const val KEY: String = "cookmaid.deeplink"
    const val DELETE_ACCOUNT: String = "delete-account"
}
```

- [ ] **Step 2: Stash the deeplink in the web entry point**

In `main.kt`, in the `else` branch of `main()`, **before** the `ComposeViewport { ... }` call, add:

```kotlin
        if (window.location.pathname == "/${Deeplink.DELETE_ACCOUNT}") {
            window.localStorage.setItem(Deeplink.KEY, Deeplink.DELETE_ACCOUNT)
        }
```

Add this import to `main.kt`:

```kotlin
import io.github.fgrutsch.cookmaid.navigation.Deeplink
```

> `window` (`kotlinx.browser.window`) is already imported. `window.localStorage` is available without extra imports.

- [ ] **Step 3: Consume the deeplink in `App.kt`**

In `App.kt`, inside `MainContent`, immediately after `val backStack = rememberNavBackStack(navConfig, Route.ShoppingList)`, add:

```kotlin
    LaunchedEffect(Unit) {
        val settings = Settings()
        if (settings.getStringOrNull(Deeplink.KEY) == Deeplink.DELETE_ACCOUNT) {
            settings.remove(Deeplink.KEY)
            backStack.add(Route.DeleteAccount)
        }
    }
```

Add these imports to `App.kt`:

```kotlin
import com.russhwolf.settings.Settings
import io.github.fgrutsch.cookmaid.navigation.Deeplink
```

> `LaunchedEffect` is already imported. `MainContent` only composes when
> `Authenticated`, so the deeplink is consumed post-login. On Android,
> `Settings().getStringOrNull` returns null (nothing wrote it) — no-op.

- [ ] **Step 4: Verify both targets compile**

Run: `./gradlew :app:shared:compileDebugKotlinAndroid :app:webApp:compileKotlinWasmJs`
Expected: PASS.

- [ ] **Step 5: Manually verify the deeplink (record the result)**

Run the dev web server: `./gradlew :app:webApp:wasmJsBrowserDevelopmentRun`

Then, in the browser:
1. While logged in, open `http://localhost:8080/delete-account` → after load you land on the Delete Account screen.
2. Log out, then open `http://localhost:8080/delete-account` → log in → you land on the Delete Account screen.

> The OIDC web flow (`WebCodeAuthFlowFactory`) uses a popup, so the main
> window stays at `/delete-account`; the localStorage stash also covers a
> full-redirect flow. If case (2) does NOT land on the screen, confirm the
> stash key is present in DevTools → Application → Local Storage and that
> `MainContent` recomposed after login. No code change expected.

- [ ] **Step 6: Commit**

```bash
git add app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/navigation/Deeplink.kt app/webApp/src/wasmJsMain/kotlin/io/github/fgrutsch/cookmaid/main.kt app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/App.kt
git commit -m "feat(web): add /delete-account deeplink with login survival"
```

---

## Task 11: Docs — FAQ entry

**Files:**
- Modify: `docs/faq.md`

- [ ] **Step 1: Add an account-deletion section**

Append to `docs/faq.md`:

```markdown
## Deleting your account

You can permanently delete your Cookmaid account and all associated data
(recipes, meal plans and shopping lists) at any time:

- **In the app:** open **Settings → Delete account**, then confirm.
- **On the web:** visit `/delete-account` (you'll be asked to sign in first).

Deletion is immediate and cannot be undone. It removes all data Cookmaid
stores about you. Your sign-in identity at the identity provider is managed
separately and is not removed by this action.
```

- [ ] **Step 2: Commit**

```bash
git add docs/faq.md
git commit -m "docs: document account deletion in FAQ"
```

---

## Task 12: Full verification

- [ ] **Step 1: Lint**

Run: `./gradlew detektAll`
Expected: PASS (no new violations).

- [ ] **Step 2: Server tests**

Run: `./gradlew :server:test`
Expected: PASS.

- [ ] **Step 3: Shared tests**

Run: `./gradlew :app:shared:allTests`
Expected: PASS.

- [ ] **Step 4: Build web + Android debug**

Run: `./gradlew :app:webApp:compileKotlinWasmJs :app:androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Final commit (if anything outstanding)**

```bash
git status
# commit any remaining changes only if present
```

---

## Notes for the owner (not code)

- Submit `https://<your-host>/delete-account` as the **account deletion URL** in
  Google Play Console → Data safety.
- Declare that account + associated data are deleted.
