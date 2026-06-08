package io.github.fgrutsch.cookmaid.ui.deleteaccount

import io.github.fgrutsch.cookmaid.support.BaseViewModelTest
import io.github.fgrutsch.cookmaid.ui.user.FakeUserClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteAccountViewModelTest : BaseViewModelTest() {

    @Test
    fun `confirm deletes the account and emits Deleted effect`() = viewModelTest {
        val client = FakeUserClient()
        val viewModel = DeleteAccountViewModel(client)

        var effect: DeleteAccountEffect? = null
        val job = launch { viewModel.effects.collect { effect = it } }

        viewModel.onEvent(DeleteAccountEvent.Confirm)
        advanceUntilIdle()

        assertTrue(client.deleteCalled)
        assertEquals(DeleteAccountEffect.Deleted, effect)
        assertFalse(viewModel.state.value.error)
        job.cancel()
    }

    @Test
    fun `confirm failure sets error and emits no effect`() = viewModelTest {
        val client = FakeUserClient().apply { failDelete = true }
        val viewModel = DeleteAccountViewModel(client)

        var effect: DeleteAccountEffect? = null
        val job = launch { viewModel.effects.collect { effect = it } }

        viewModel.onEvent(DeleteAccountEvent.Confirm)
        advanceUntilIdle()

        assertNull(effect)
        assertTrue(viewModel.state.value.error)
        assertFalse(viewModel.state.value.deleting)
        job.cancel()
    }
}
