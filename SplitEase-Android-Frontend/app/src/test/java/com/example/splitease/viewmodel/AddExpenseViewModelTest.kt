package com.example.splitease.viewmodel

import com.example.splitease.ui.formatAmount
import com.example.splitease.bill.ScannedBill
import com.example.splitease.data.AppDataStore
import com.example.splitease.dto.SplitEntryDto
import com.example.splitease.testutil.FakeBillScanner
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
    private val billScanner = FakeBillScanner()
    private val viewModel = AddExpenseViewModel(repository, dataStore, billScanner)

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

    // Bill photo

    @Test
    fun attachingABillFillsInTheTotalFoundOnIt() = runTest(mainRule.dispatcher) {
        viewModel.attachBill("content://bill")
        assertEquals(BillScanStatus.READING, viewModel.addExpenseUiState.billScanStatus)
        advanceUntilIdle()

        val state = viewModel.addExpenseUiState
        assertEquals("content://bill", state.billUri)
        assertEquals(BillScanStatus.FOUND_AMOUNT, state.billScanStatus)
        assertEquals("450.00", state.detectedAmount)
        assertEquals("450.00", state.amount)
        assertEquals(true, billScanner.lastReadTotal)
    }

    @Test
    fun aTypedAmountIsKeptButTheBillTotalCanBeUsedInstead() = runTest(mainRule.dispatcher) {
        fillIn(amount = "400")

        viewModel.attachBill("content://bill")
        advanceUntilIdle()
        assertEquals("400", viewModel.addExpenseUiState.amount)
        assertEquals("450.00", viewModel.addExpenseUiState.detectedAmount)

        viewModel.useDetectedAmount()
        assertEquals("450.00", viewModel.addExpenseUiState.amount)
    }

    @Test
    fun replacingTheBillReplacesTheAmountItFilledIn() = runTest(mainRule.dispatcher) {
        viewModel.attachBill("content://first")
        advanceUntilIdle()
        billScanner.result = { ScannedBill(byteArrayOf(9), detectedAmount = 1234.5) }

        viewModel.attachBill("content://second")
        advanceUntilIdle()

        assertEquals("1234.50", viewModel.addExpenseUiState.amount)
        assertEquals("content://second", viewModel.addExpenseUiState.billUri)
    }

    @Test
    fun billWithoutAReadableTotalLeavesTheAmountAlone() = runTest(mainRule.dispatcher) {
        billScanner.result = { ScannedBill(byteArrayOf(1), detectedAmount = null) }

        viewModel.attachBill("content://bill")
        advanceUntilIdle()

        assertEquals(BillScanStatus.NO_AMOUNT, viewModel.addExpenseUiState.billScanStatus)
        assertEquals("", viewModel.addExpenseUiState.amount)
        assertEquals("content://bill", viewModel.addExpenseUiState.billUri)
    }

    @Test
    fun photoThatCannotBeOpenedIsDropped() = runTest(mainRule.dispatcher) {
        billScanner.result = { throw IOException("Not an image") }

        viewModel.attachBill("content://broken")
        advanceUntilIdle()

        assertNull(viewModel.addExpenseUiState.billUri)
        assertNull(viewModel.addExpenseUiState.billScanStatus)
        assertEquals("Couldn't open that photo. Try another one.", viewModel.addExpenseUiState.errorMessage)
    }

    @Test
    fun cannotSubmitWhileTheBillIsBeingRead() = runTest(mainRule.dispatcher) {
        val gate = billScanner.suspendUntilCompleted(ScannedBill(byteArrayOf(1), 450.0))
        viewModel.attachBill("content://bill")
        advanceUntilIdle()
        fillIn()

        viewModel.addExpense("g-1") {}
        advanceUntilIdle()

        assertEquals("Wait for the bill photo to finish reading", viewModel.addExpenseUiState.errorMessage)
        assertEquals(0, repository.callCount("addExpense"))
        gate.complete(Unit)
    }

    @Test
    fun billIsUploadedToTheNewExpense() = runTest(mainRule.dispatcher) {
        val toasts = recordToasts()
        viewModel.attachBill("content://bill")
        advanceUntilIdle()
        fillIn(amount = "450.00")
        var succeeded = false

        viewModel.addExpense("g-1") { succeeded = true }
        advanceUntilIdle()

        val (expenseId, jpeg) = repository.lastReceiptUpload!!
        assertEquals("e-new", expenseId)
        assertTrue(jpeg.contentEquals(byteArrayOf(1, 2, 3)))
        assertTrue(succeeded)
        assertEquals(listOf("Expense \"Dinner\" added"), toasts)
        assertEquals("reloaded after the upload so the bill shows up", 1, repository.storeLoadCount())
    }

    @Test
    fun failedUploadStillAddsTheExpenseAndSaysSo() = runTest(mainRule.dispatcher) {
        val toasts = recordToasts()
        repository.uploadReceipt = { _, _ -> throw IOException("timeout") }
        viewModel.attachBill("content://bill")
        advanceUntilIdle()
        fillIn()
        var succeeded = false

        viewModel.addExpense("g-1") { succeeded = true }
        advanceUntilIdle()

        assertTrue(succeeded)
        assertEquals(
            listOf("Expense \"Dinner\" added, but the bill photo couldn't be uploaded. The payer can attach it from the expense's page."),
            toasts
        )
    }

    @Test
    fun removedBillIsNotUploaded() = runTest(mainRule.dispatcher) {
        viewModel.attachBill("content://bill")
        advanceUntilIdle()
        viewModel.removeBill()
        fillIn()

        viewModel.addExpense("g-1") {}
        advanceUntilIdle()

        assertNull(viewModel.addExpenseUiState.billUri)
        assertEquals(1, repository.callCount("addExpense"))
        assertEquals(0, repository.callCount("uploadReceipt"))
    }

    @Test
    fun resetClearsTheBillToo() = runTest(mainRule.dispatcher) {
        viewModel.attachBill("content://bill")
        advanceUntilIdle()

        viewModel.resetUiState()
        fillIn()
        viewModel.addExpense("g-1") {}
        advanceUntilIdle()

        assertEquals(0, repository.callCount("uploadReceipt"))
    }

    @Test
    fun amountsAreFormattedSoTheAmountFieldCanReadThemBack() {
        assertEquals("450.00", formatAmount(450.0))
        assertEquals("1234.50", formatAmount(1234.5))
        assertEquals("0.10", formatAmount(0.1))
    }

    @Test
    fun resetClearsTheForm() {
        viewModel.setGroupMembers(listOf(asha))
        fillIn()

        viewModel.resetUiState()

        assertEquals(AddExpenseUiState(), viewModel.addExpenseUiState)
    }
}
