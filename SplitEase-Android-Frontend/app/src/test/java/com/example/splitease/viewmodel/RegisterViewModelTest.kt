package com.example.splitease.viewmodel

import com.example.splitease.testutil.FakeRepository
import com.example.splitease.testutil.Fixtures
import com.example.splitease.testutil.MainDispatcherRule
import com.example.splitease.testutil.recordToasts
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeRepository()

    private val valid = UserDetails(
        name = "Asha", email = "asha@gmail.com", mobileNumber = "9876543210",
        password = "secret1", confirmPassword = "secret1"
    )

    @Test
    fun entryIsValidOnlyWithAllRulesMet() {
        val viewModel = RegisterViewModel(repository)
        val invalid = listOf(
            valid.copy(name = ""),
            valid.copy(email = "asha@yahoo.com"),
            valid.copy(mobileNumber = "12345"),
            valid.copy(confirmPassword = "different"),
            valid.copy(password = "", confirmPassword = "")
        )

        viewModel.updateUiState(valid)
        assertTrue(viewModel.registerUiState.isEntryValid)
        for (details in invalid) {
            viewModel.updateUiState(details)
            assertFalse(details.toString(), viewModel.registerUiState.isEntryValid)
        }
    }

    @Test
    fun invalidEntryIsNotSent() = runTest(mainRule.dispatcher) {
        val viewModel = RegisterViewModel(repository)
        viewModel.updateUiState(valid.copy(email = "asha@yahoo.com"))

        viewModel.register()
        advanceUntilIdle()

        assertEquals(0, repository.callCount("register"))
    }

    @Test
    fun successSendsDetailsToastsAndGoesToLogin() = runTest(mainRule.dispatcher) {
        val toasts = recordToasts()
        val viewModel = RegisterViewModel(repository)
        val navigated = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.navigateToLoginScreen.first() }
        viewModel.updateUiState(valid)

        viewModel.register()
        advanceUntilIdle()

        assertTrue(navigated.isCompleted)
        assertEquals(1, repository.callCount("register"))
        assertEquals(listOf("Account created, please log in"), toasts)
        assertTrue(viewModel.registerState is RegisterState.Success)
    }

    @Test
    fun serverErrorToastsAndStays() = runTest(mainRule.dispatcher) {
        val toasts = recordToasts()
        repository.register = { throw Fixtures.httpException(409, "Email already registered") }
        val viewModel = RegisterViewModel(repository)
        viewModel.updateUiState(valid)

        viewModel.register()
        advanceUntilIdle()

        assertEquals(listOf("Registration failed: Email already registered"), toasts)
        assertTrue(viewModel.registerState is RegisterState.Error)
    }
}
