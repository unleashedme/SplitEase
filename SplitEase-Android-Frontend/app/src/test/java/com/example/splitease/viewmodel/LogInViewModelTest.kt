package com.example.splitease.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.splitease.datastore.AuthStore
import com.example.splitease.testutil.FakeRepository
import com.example.splitease.testutil.Fixtures
import com.example.splitease.testutil.MainDispatcherRule
import com.example.splitease.testutil.recordToasts
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/** Uses Robolectric because AuthStore persists the login with a real DataStore. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LogInViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeRepository()
    private val authStore = AuthStore(ApplicationProvider.getApplicationContext())
    private val viewModel = LogInViewModel(repository, authStore)

    @Test
    fun entryNeedsAGmailAddressAndPassword() {
        viewModel.updateUiState(LogInDetails(email = "asha@gmail.com", password = "secret"))
        assertTrue(viewModel.logInUiState.isEntryValid)

        viewModel.updateUiState(LogInDetails(email = "asha@yahoo.com", password = "secret"))
        assertFalse(viewModel.logInUiState.isEntryValid)

        viewModel.updateUiState(LogInDetails(email = "asha@gmail.com", password = ""))
        assertFalse(viewModel.logInUiState.isEntryValid)
    }

    @Test
    fun successSavesTheSessionToastsAndGoesToDashboard() = runTest(mainRule.dispatcher) {
        val toasts = recordToasts()
        val navigated = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.navigateToDashboard.first() }
        viewModel.updateUiState(LogInDetails(email = "asha@gmail.com", password = "secret"))

        viewModel.logIn()
        // Saving goes through DataStore on a real IO thread, so wait for the navigation event
        navigated.await()
        advanceUntilIdle()

        assertEquals("token-123", authStore.tokenFlow.first())
        assertEquals(listOf("Welcome back, Asha!"), toasts)
        assertTrue(viewModel.loginState is LoginState.Success)
    }

    @Test
    fun wrongPasswordSaysSo() = runTest(mainRule.dispatcher) {
        val toasts = recordToasts()
        repository.login = { _, _ -> throw Fixtures.httpException(401) }
        viewModel.updateUiState(LogInDetails(email = "asha@gmail.com", password = "wrong"))

        viewModel.logIn()
        advanceUntilIdle()

        assertEquals(listOf("Login failed: wrong email or password"), toasts)
        assertTrue(viewModel.loginState is LoginState.Error)
        assertNull("nothing saved", authStore.tokenFlow.first())
    }

    @Test
    fun offlineSaysToCheckTheConnection() = runTest(mainRule.dispatcher) {
        val toasts = recordToasts()
        repository.login = { _, _ -> throw IOException("offline") }
        viewModel.updateUiState(LogInDetails(email = "asha@gmail.com", password = "secret"))

        viewModel.logIn()
        advanceUntilIdle()

        assertEquals(listOf("Login failed: check your internet connection"), toasts)
    }

    @Test
    fun invalidEntryIsNotSent() = runTest(mainRule.dispatcher) {
        viewModel.updateUiState(LogInDetails(email = "asha@yahoo.com", password = "secret"))

        viewModel.logIn()
        advanceUntilIdle()

        assertEquals(0, repository.callCount("login"))
    }
}
