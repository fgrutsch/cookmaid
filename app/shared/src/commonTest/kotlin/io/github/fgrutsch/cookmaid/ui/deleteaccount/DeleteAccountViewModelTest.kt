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
