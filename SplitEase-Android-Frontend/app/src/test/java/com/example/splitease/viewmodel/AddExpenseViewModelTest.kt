package com.example.splitease.viewmodel

import com.example.splitease.data.AppDataStore
import com.example.splitease.dto.SplitEntryDto
import com.example.splitease.testutil.FakeRepository
import com.example.splitease.testutil.Fixtures
import com.example.splitease.testutil.Fixtures.asha
import com.example.splitease.testutil.Fixtures.ravi
import com.example.splitease.testutil.MainDispatcherRule
import com.example.splitease.testutil.recordToasts
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class AddExpenseViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeRepository()
    private val dataStore = AppDataStore(repository)
    private val viewModel = AddExpenseViewModel(repository, dataStore)

    @After
    fun tearDown() = dataStore.close()

    private fun fillIn(amount: String = "200", description: String = "Dinner") {
        viewModel.updateUiState(viewModel.addExpenseUiState.copy(amount = amount, description = description))
    }

    @Test
    fun pickingAGroupIncludesEveryMember() {
        viewModel.setGroupMembers(listOf(asha, ravi))

        val state = viewModel.addExpenseUiState
        assertEquals(listOf(asha, ravi), state.members)
        assertEquals(setOf(asha.userId, ravi.userId), state.includedMemberIds)
    }

    @Test
    fun switchingToSharesDefaultsEveryoneToOneShare() {
        viewModel.setGroupMembers(listOf(asha, ravi))
        viewModel.setSplitMode(SplitMode.SHARES)

        assertEquals(mapOf(asha.userId to "1", ravi.userId to "1"), viewModel.addExpenseUiState.splitValues)

        viewModel.setSplitMode(SplitMode.EXACT)
        assertTrue(viewModel.addExpenseUiState.splitValues.isEmpty())
    }

    @Test
    fun toggleMemberAddsAndRemovesThem() {
        viewModel.setGroupMembers(listOf(asha, ravi))

        viewModel.toggleMember(ravi.userId)
        assertEquals(setOf(asha.userId), viewModel.addExpenseUiState.includedMemberIds)

        viewModel.toggleMember(ravi.userId)
        assertEquals(setOf(asha.userId, ravi.userId), viewModel.addExpenseUiState.includedMemberIds)
    }

    @Test
    fun editingClearsThePreviousError() = runTest(mainRule.dispatcher) {
        viewModel.addExpense(groupId = "") {}
        assertEquals("Select a group", viewModel.addExpenseUiState.errorMessage)

        viewModel.updateSplitValue(asha.userId, "10")
        assertNull(viewModel.addExpenseUiState.errorMessage)
    }

    @Test
    fun invalidInputShowsAnErrorAndSendsNothing() = runTest(mainRule.dispatcher) {
        val cases = listOf(
            Triple("", "200", "Dinner") to "Select a group",
            Triple("g-1", "200", " ") to "Enter a description",
            Triple("g-1", "abc", "Dinner") to "Enter an amount greater than zero",
            Triple("g-1", "0", "Dinner") to "Enter an amount greater than zero"
        )
        for ((input, expectedError) in cases) {
            val (groupId, amount, description) = input
            fillIn(amount, description)
            var succeeded = false

            viewModel.addExpense(groupId) { succeeded = true }
            advanceUntilIdle()

            assertEquals(expectedError, viewModel.addExpenseUiState.errorMessage)
            assertFalse(succeeded)
        }
        assertEquals(0, repository.callCount("addExpense"))
    }

    @Test
    fun invalidSplitIsCaughtBeforeSending() = runTest(mainRule.dispatcher) {
        viewModel.setGroupMembers(listOf(asha, ravi))
        viewModel.setSplitMode(SplitMode.PERCENTAGE)
        viewModel.updateSplitValue(asha.userId, "50")
        fillIn()

        viewModel.addExpense("g-1") {}
        advanceUntilIdle()

        assertTrue(viewModel.addExpenseUiState.errorMessage!!.startsWith("Percentages add up to"))
        assertEquals(0, repository.callCount("addExpense"))
    }

    @Test
    fun successSendsTheSplitToastsAndRefreshesCachedData() = runTest(mainRule.dispatcher) {
        val toasts = recordToasts()
        viewModel.setGroupMembers(listOf(asha, ravi))
        viewModel.setSplitMode(SplitMode.EXACT)
        viewModel.updateSplitValue(asha.userId, "150")
        viewModel.updateSplitValue(ravi.userId, "50")
        fillIn(amount = "200", description = "Dinner")
        var succeeded = false

        viewModel.addExpense("g-1") { succeeded = true }
        advanceUntilIdle()

        val sent = repository.lastAddExpense!!
        assertEquals("g-1", sent.groupId)
        assertEquals(200.0, sent.amount, 0.0)
        assertEquals("EXACT", sent.splitType)
        assertEquals(listOf(SplitEntryDto(asha.userId, 150.0), SplitEntryDto(ravi.userId, 50.0)), sent.splits)
        assertTrue(succeeded)
        assertEquals(listOf("Expense \"Dinner\" added"), toasts)
        assertEquals("cached screens reloaded once", 1, repository.storeLoadCount())
    }

    @Test
    fun serverRejectionShowsItsReasonAndDoesNotFinish() = runTest(mainRule.dispatcher) {
        val toasts = recordToasts()
        repository.addExpense = { Fixtures.errorResponse(400, "Percentages add up to 90 but must add up to 100") }
        fillIn()
        var succeeded = false

        viewModel.addExpense("g-1") { succeeded = true }
        advanceUntilIdle()

        assertFalse(succeeded)
        assertEquals(listOf("Couldn't add expense: Percentages add up to 90 but must add up to 100"), toasts)
        assertEquals("no reload after a failure", 0, repository.storeLoadCount())
    }

    @Test
    fun networkFailureShowsAConnectionToast() = runTest(mainRule.dispatcher) {
        val toasts = recordToasts()
        repository.addExpense = { throw IOException("timeout") }
        fillIn()

        viewModel.addExpense("g-1") {}
        advanceUntilIdle()

        assertEquals(listOf("Couldn't add expense: check your internet connection"), toasts)
    }

    @Test
    fun resetClearsTheForm() {
        viewModel.setGroupMembers(listOf(asha))
        fillIn()

        viewModel.resetUiState()

        assertEquals(AddExpenseUiState(), viewModel.addExpenseUiState)
    }
}
