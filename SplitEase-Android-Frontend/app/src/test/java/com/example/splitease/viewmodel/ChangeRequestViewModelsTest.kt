package com.example.splitease.viewmodel

import com.example.splitease.data.AppDataStore
import com.example.splitease.testutil.FakeBillScanner
import com.example.splitease.testutil.FakeRepository
import com.example.splitease.testutil.Fixtures
import com.example.splitease.testutil.MainDispatcherRule
import com.example.splitease.testutil.recordToasts
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * Creating/deleting groups, deleting expenses and settling up: each should toast the outcome,
 * and only a success should finish the dialog and reload the cached screens.
 */
class ChangeRequestViewModelsTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeRepository()
    private val store = AppDataStore(repository)

    @After
    fun tearDown() = store.close()

    private class Outcome(val toasts: List<String>, val succeeded: Boolean)

    /** Runs [action] (which gets the onSuccess callback) and reports what happened. */
    private fun TestScope.perform(action: (onSuccess: () -> Unit) -> Unit): Outcome {
        val toasts = recordToasts()
        var succeeded = false
        action { succeeded = true }
        advanceUntilIdle()
        return Outcome(toasts, succeeded)
    }

    // Create group

    @Test
    fun createGroupSuccess() = runTest(mainRule.dispatcher) {
        val viewModel = CreateGroupViewModel(repository, store)
        viewModel.updateUiState(CreateGroupUiState(name = "Trip", members = listOf("ravi@gmail.com")))

        val outcome = perform { viewModel.createGroup(it) }

        assertTrue(outcome.succeeded)
        assertEquals(listOf("Group \"Trip\" created"), outcome.toasts)
        assertEquals(1, repository.storeLoadCount())
    }

    @Test
    fun createGroupServerErrorIsNotTreatedAsSuccess() = runTest(mainRule.dispatcher) {
        repository.createGroup = { _, _ -> Fixtures.errorResponse(400, "User not found: ghost@gmail.com") }
        val viewModel = CreateGroupViewModel(repository, store)
        viewModel.updateUiState(CreateGroupUiState(name = "Trip", members = listOf("ghost@gmail.com")))

        val outcome = perform { viewModel.createGroup(it) }

        assertFalse(outcome.succeeded)
        assertEquals(listOf("Couldn't create group: User not found: ghost@gmail.com"), outcome.toasts)
        assertEquals(0, repository.storeLoadCount())
    }

    @Test
    fun createGroupMemberFields() {
        val viewModel = CreateGroupViewModel(repository, store)

        viewModel.addMember()
        viewModel.updateMemberEmail(1, "ravi@gmail.com")
        repeat(5) { viewModel.addMember() }

        assertEquals("at most 4 member fields", 4, viewModel.createGroupUiState.members.size)
        assertEquals("ravi@gmail.com", viewModel.createGroupUiState.members[1])

        viewModel.resetUiState()
        assertEquals(CreateGroupUiState(), viewModel.createGroupUiState)
    }

    // Delete group

    @Test
    fun deleteGroupSuccess() = runTest(mainRule.dispatcher) {
        val viewModel = GroupViewModel(repository, store)
        advanceUntilIdle()

        val outcome = perform { viewModel.deleteGroup("g-1", it) }

        assertTrue(outcome.succeeded)
        assertEquals(listOf("Group deleted"), outcome.toasts)
        assertEquals("initial load + reload after delete", 2, repository.storeLoadCount())
    }

    @Test
    fun deleteGroupServerErrorIsNotTreatedAsSuccess() = runTest(mainRule.dispatcher) {
        repository.deleteGroup = { Fixtures.errorResponse(403, "Only the group admin can delete it") }
        val viewModel = GroupViewModel(repository, store)
        advanceUntilIdle()

        val outcome = perform { viewModel.deleteGroup("g-1", it) }

        assertFalse(outcome.succeeded)
        assertEquals(listOf("Couldn't delete group: Only the group admin can delete it"), outcome.toasts)
        assertEquals(1, repository.storeLoadCount())
    }

    // Delete expense

    @Test
    fun deleteExpenseSuccess() = runTest(mainRule.dispatcher) {
        val viewModel = ActivityViewModel(repository, store, FakeBillScanner())
        advanceUntilIdle()

        val outcome = perform { viewModel.deleteExpense("e-1", it) }

        assertTrue(outcome.succeeded)
        assertEquals(listOf("Expense deleted"), outcome.toasts)
        assertEquals(2, repository.storeLoadCount())
    }

    @Test
    fun deleteExpenseByNonPayerFails() = runTest(mainRule.dispatcher) {
        repository.deleteExpense = { Fixtures.errorResponse(403, "Unauthorized") }
        val viewModel = ActivityViewModel(repository, store, FakeBillScanner())
        advanceUntilIdle()

        val outcome = perform { viewModel.deleteExpense("e-1", it) }

        assertFalse(outcome.succeeded)
        assertEquals(listOf("Couldn't delete expense: Unauthorized"), outcome.toasts)
        assertEquals(1, repository.storeLoadCount())
    }

    // Bill photo on an existing expense

    @Test
    fun attachBillUploadsPhotoWithoutReadingIt() = runTest(mainRule.dispatcher) {
        val scanner = FakeBillScanner()
        val viewModel = ActivityViewModel(repository, store, scanner)
        advanceUntilIdle()
        val toasts = recordToasts()

        viewModel.attachBill("e-1", "content://bill")
        advanceUntilIdle()

        assertEquals(false, scanner.lastReadTotal)
        assertEquals("e-1", repository.lastReceiptUpload!!.first)
        assertEquals(listOf("Bill photo attached"), toasts)
        assertEquals("image cache told the photo changed", 1, viewModel.billRevision)
        assertEquals(null, viewModel.billChangeInProgressFor)
        assertEquals(2, repository.storeLoadCount())
    }

    @Test
    fun attachBillRejectedByServerShowsReason() = runTest(mainRule.dispatcher) {
        repository.uploadReceipt = { _, _ -> Fixtures.errorResponse(403, "Only the person who paid can change the bill") }
        val viewModel = ActivityViewModel(repository, store, FakeBillScanner())
        advanceUntilIdle()
        val toasts = recordToasts()

        viewModel.attachBill("e-1", "content://bill")
        advanceUntilIdle()

        assertEquals(listOf("Couldn't upload bill photo: Only the person who paid can change the bill"), toasts)
        assertEquals(0, viewModel.billRevision)
        assertEquals(null, viewModel.billChangeInProgressFor)
        assertEquals(1, repository.storeLoadCount())
    }

    @Test
    fun removeBillSuccess() = runTest(mainRule.dispatcher) {
        val viewModel = ActivityViewModel(repository, store, FakeBillScanner())
        advanceUntilIdle()
        val toasts = recordToasts()

        viewModel.removeBill("e-1")
        advanceUntilIdle()

        assertEquals(1, repository.callCount("deleteReceipt"))
        assertEquals(listOf("Bill photo removed"), toasts)
        assertEquals(1, viewModel.billRevision)
        assertEquals(2, repository.storeLoadCount())
    }

    // Settle up

    @Test
    fun recordPaymentSuccess() = runTest(mainRule.dispatcher) {
        val viewModel = SettlementViewModel(repository, store)
        advanceUntilIdle()

        val outcome = perform { viewModel.recordPayment("u-ravi", 250.0, "Paid via UPI", it) }

        assertTrue(outcome.succeeded)
        assertEquals("u-ravi", repository.lastSettlement!!.toUserId)
        assertEquals(250.0, repository.lastSettlement!!.amount, 0.0)
        assertEquals("Paid via UPI", repository.lastSettlement!!.note)
        assertEquals(1, outcome.toasts.size)
        assertTrue(outcome.toasts[0].startsWith("Payment of ₹250"))
        assertEquals(2, repository.storeLoadCount())
    }

    @Test
    fun recordPaymentFailure() = runTest(mainRule.dispatcher) {
        repository.recordSettlement = { throw IOException("offline") }
        val viewModel = SettlementViewModel(repository, store)
        advanceUntilIdle()

        val outcome = perform { viewModel.recordPayment("u-ravi", 250.0, "", it) }

        assertFalse(outcome.succeeded)
        assertEquals(listOf("Couldn't record payment: check your internet connection"), outcome.toasts)
        assertEquals(1, repository.storeLoadCount())
    }

    @Test
    fun recordPaymentWithStaleAmountReloadsWhatIsOwed() = runTest(mainRule.dispatcher) {
        repository.recordSettlement = {
            throw Fixtures.httpException(409, "Balances have changed: you now owe Ravi ₹300.00. Refresh and try again.")
        }
        val viewModel = SettlementViewModel(repository, store)
        advanceUntilIdle()

        val outcome = perform { viewModel.recordPayment("u-ravi", 250.0, "", it) }

        assertFalse(outcome.succeeded)
        assertEquals(
            listOf("Couldn't record payment: Balances have changed: you now owe Ravi ₹300.00. Refresh and try again."),
            outcome.toasts
        )
        assertEquals("reloaded so the dialog shows the new amount", 2, repository.storeLoadCount())
    }
}
