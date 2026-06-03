package io.github.fgrutsch.cookmaid.support

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseViewModelTest {

    protected val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUpDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDownDispatcher() {
        Dispatchers.resetMain()
    }

    protected fun viewModelTest(block: suspend TestScope.() -> Unit) =
        runTest(testDispatcher) {
            block()
            advanceUntilIdle()
        }
}
