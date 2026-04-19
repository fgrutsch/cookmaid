package io.github.fgrutsch.cookmaid.ui.auth

import io.github.fgrutsch.cookmaid.support.BaseViewModelTest
import io.github.fgrutsch.cookmaid.user.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest : BaseViewModelTest() {

    private val fakeHandler = FakeAuthHandler()

    private fun TestScope.createInitializedViewModel(): AuthViewModel {
        val viewModel = AuthViewModel(fakeHandler)
        viewModel.onEvent(AuthEvent.Initialize)
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun `starts in initializing state`() = viewModelTest {
        val viewModel = AuthViewModel(fakeHandler)

        assertEquals(AuthState.Status.Initializing, viewModel.state.value.status)
    }

    @Test
    fun `initialize auto-login success transitions to authenticated`() = viewModelTest {
        val user = User(id = Uuid.random(), oidcSubject = "sub-1")
        fakeHandler.resultToReturn = AuthResult(user, UserProfile(name = "John"))

        val viewModel = createInitializedViewModel()

        val state = viewModel.state.value
        assertEquals(AuthState.Status.Authenticated, state.status)
        assertEquals(user, state.user)
        assertEquals("John", state.profile.name)
    }

    @Test
    fun `initialize auto-login failure transitions to unauthenticated`() = viewModelTest {
        fakeHandler.shouldFail = true

        val viewModel = createInitializedViewModel()

        assertEquals(AuthState.Status.Unauthenticated, viewModel.state.value.status)
    }

    @Test
    fun `login success transitions to authenticated`() = viewModelTest {
        fakeHandler.shouldFail = true
        val viewModel = createInitializedViewModel()

        val user = User(id = Uuid.random(), oidcSubject = "sub-2")
        fakeHandler.shouldFail = false
        fakeHandler.resultToReturn = AuthResult(user, UserProfile(name = "Jane"))

        viewModel.onEvent(AuthEvent.Login)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(AuthState.Status.Authenticated, state.status)
        assertEquals(user, state.user)
    }

    @Test
    fun `login failure sets error message`() = viewModelTest {
        fakeHandler.shouldFail = true
        fakeHandler.failMessage = "Invalid credentials"
        val viewModel = createInitializedViewModel()

        viewModel.onEvent(AuthEvent.Login)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(AuthState.Status.Unauthenticated, state.status)
        assertEquals("Invalid credentials", state.loginError)
    }

    @Test
    fun `logout transitions to unauthenticated`() = viewModelTest {
        val user = User(id = Uuid.random(), oidcSubject = "sub-1")
        fakeHandler.resultToReturn = AuthResult(user, UserProfile())
        val viewModel = createInitializedViewModel()
        assertEquals(AuthState.Status.Authenticated, viewModel.state.value.status)

        viewModel.onEvent(AuthEvent.Logout)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(AuthState.Status.Unauthenticated, state.status)
        assertNull(state.user)
    }

    @Test
    fun `logout resets identity synchronously before handler runs`() = viewModelTest {
        val user = User(id = Uuid.random(), oidcSubject = "sub-1")
        fakeHandler.resultToReturn = AuthResult(user, UserProfile(name = "Alice"))
        val viewModel = createInitializedViewModel()

        viewModel.onEvent(AuthEvent.Logout)

        val state = viewModel.state.value
        assertEquals(AuthState.Status.Unauthenticated, state.status)
        assertNull(state.user)
        assertEquals(UserProfile(), state.profile)
        assertNull(state.loginError)
    }

    @Test
    fun `login clears previous error`() = viewModelTest {
        fakeHandler.shouldFail = true
        fakeHandler.failMessage = "Old error"
        val viewModel = createInitializedViewModel()

        viewModel.onEvent(AuthEvent.Login)
        advanceUntilIdle()
        assertEquals("Old error", viewModel.state.value.loginError)

        fakeHandler.shouldFail = false
        fakeHandler.resultToReturn = AuthResult(
            User(id = Uuid.random(), oidcSubject = "sub-1"),
            UserProfile(),
        )
        viewModel.onEvent(AuthEvent.Login)
        advanceUntilIdle()

        assertNull(viewModel.state.value.loginError)
    }
}
