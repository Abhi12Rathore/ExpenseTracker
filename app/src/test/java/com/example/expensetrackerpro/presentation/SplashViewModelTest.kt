package com.example.expensetrackerpro.presentation

import com.example.expensetrackerpro.presentation.splash.SplashViewModel
import com.example.expensetrackerpro.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun becomesReadyAfter1800ms() = runTest {
        val viewModel = SplashViewModel()
        assertFalse(viewModel.uiState.value.isReady)

        advanceTimeBy(1799)
        assertFalse(viewModel.uiState.value.isReady)

        advanceTimeBy(2)
        assertTrue(viewModel.uiState.value.isReady)
    }
}
