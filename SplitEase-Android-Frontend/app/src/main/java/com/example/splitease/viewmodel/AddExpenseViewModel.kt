package com.example.splitease.viewmodel

import com.example.splitease.ui.formatAmount
import com.example.splitease.ui.rupees
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.splitease.bill.BillScanner
import com.example.splitease.bill.MlKitBillScanner
import com.example.splitease.bill.ScannedBill
import com.example.splitease.data.AppDataStore
import com.example.splitease.data.Repository
import com.example.splitease.dto.SplitEntryDto
import com.example.splitease.ui.AppApplication
import com.example.splitease.ui.RequestToasts
import com.example.splitease.ui.model.GroupMemberDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

class AddExpenseViewModel(
    private val repository : Repository,
    private val dataStore: AppDataStore,
    private val billScanner: BillScanner
) : ViewModel() {

    var addExpenseUiState by mutableStateOf(AddExpenseUiState())
        private set

    // The compressed photo waiting to be uploaded once the expense is created
    private var scannedBill: ScannedBill? = null
    private var scanJob: Job? = null

    fun updateUiState(addExpenseUiState: AddExpenseUiState){
        this.addExpenseUiState = addExpenseUiState.copy(errorMessage = null)
    }

    fun resetUiState(){
        clearBill()
        addExpenseUiState = AddExpenseUiState()
    }

    fun attachBill(uri: String){
        scanJob?.cancel()
        scannedBill = null
        addExpenseUiState = addExpenseUiState.copy(
            billUri = uri, billScanStatus = BillScanStatus.READING, errorMessage = null
        )
        scanJob = viewModelScope.launch {
            try {
                val bill = billScanner.scan(uri)
                scannedBill = bill
                val state = addExpenseUiState
                val detected = bill.detectedAmount?.let(::formatAmount)
                // Fill the amount in unless the user has already typed their own
                val amountIsUntouched = state.amount.isBlank() || state.amount == state.detectedAmount
                addExpenseUiState = state.copy(
                    amount = if (detected != null && amountIsUntouched) detected else state.amount,
                    detectedAmount = detected,
                    billScanStatus = if (detected != null) BillScanStatus.FOUND_AMOUNT else BillScanStatus.NO_AMOUNT
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("BILL SCAN: exception = ${e::class.java} ${e.message}")
                addExpenseUiState = addExpenseUiState.copy(
                    billUri = null, billScanStatus = null, detectedAmount = null,
                    errorMessage = "Couldn't open that photo. Try another one."
                )
            }
        }
    }

    fun useDetectedAmount(){
        val detected = addExpenseUiState.detectedAmount ?: return
        addExpenseUiState = addExpenseUiState.copy(amount = detected, errorMessage = null)
    }

    fun removeBill(){
        clearBill()
        addExpenseUiState = addExpenseUiState.copy(billUri = null, billScanStatus = null, detectedAmount = null)
    }

    private fun clearBill(){
        scanJob?.cancel()
        scanJob = null
        scannedBill = null
    }

    // Called when a group is picked: everyone in the group takes part by default
    fun setGroupMembers(members: List<GroupMemberDto>){
        addExpenseUiState = addExpenseUiState.copy(
            members = members,
            includedMemberIds = members.map { it.userId }.toSet(),
            splitValues = defaultSplitValues(addExpenseUiState.splitMode, members),
            errorMessage = null
        )
    }

    fun setSplitMode(splitMode: SplitMode){
        addExpenseUiState = addExpenseUiState.copy(
            splitMode = splitMode,
            splitValues = defaultSplitValues(splitMode, addExpenseUiState.members),
            errorMessage = null
        )
    }

    fun toggleMember(userId: String){
        val included = addExpenseUiState.includedMemberIds
        addExpenseUiState = addExpenseUiState.copy(
            includedMemberIds = if (userId in included) included - userId else included + userId,
            errorMessage = null
        )
    }

    fun updateSplitValue(userId: String, value: String){
        addExpenseUiState = addExpenseUiState.copy(
            splitValues = addExpenseUiState.splitValues + (userId to value),
            errorMessage = null
        )
    }

    private fun defaultSplitValues(splitMode: SplitMode, members: List<GroupMemberDto>): Map<String, String> =
        if (splitMode == SplitMode.SHARES) members.associate { it.userId to "1" } else emptyMap()

    fun addExpense(groupId: String, onSuccess:() -> Unit){
        val state = addExpenseUiState
        val error = when {
            groupId.isBlank() -> "Select a group"
            state.description.isBlank() -> "Enter a description"
            (state.amount.toDoubleOrNull() ?: 0.0) <= 0.0 -> "Enter an amount greater than zero"
            state.billScanStatus == BillScanStatus.READING -> "Wait for the bill photo to finish reading"
            else -> state.splitError()
        }
        if (error != null) {
            addExpenseUiState = state.copy(errorMessage = error)
            return
        }
        viewModelScope.launch {
            try {
                val response = repository.addExpense(
                    groupId = groupId,
                    amount = state.amount.toDouble(),
                    description = state.description,
                    splitType = state.splitMode.name,
                    splits = state.splitEntries()
                )
                if (!response.isSuccessful) {
                    RequestToasts.failure("Couldn't add expense", response)
                    return@launch
                }
                println("Add Expense called is Successfully")
                val bill = scannedBill
                if (bill == null || uploadBill(response.body()?.expenseId, bill)) {
                    RequestToasts.success("Expense \"${state.description}\" added")
                } else {
                    RequestToasts.failure(
                        "Expense \"${state.description}\" added, but the bill photo couldn't be uploaded. " +
                                "The payer can attach it from the expense's page."
                    )
                }
                dataStore.refresh()
                onSuccess()
            }catch (e: Exception){
                println("ADD EXPENSE: exception = ${e::class.java} ${e.message}")
                RequestToasts.failure("Couldn't add expense", e)
            }
        }
    }

    // A failed upload doesn't undo the expense; the bill can be attached again from its page
    private suspend fun uploadBill(expenseId: String?, bill: ScannedBill): Boolean {
        if (expenseId == null) return false
        return try {
            repository.uploadReceipt(expenseId, bill.jpeg).isSuccessful
        } catch (e: Exception) {
            println("BILL UPLOAD: exception = ${e::class.java} ${e.message}")
            false
        }
    }

    companion object{
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as AppApplication)
                val repository = application.container.repository
                AddExpenseViewModel(
                    repository = repository,
                    dataStore = application.container.dataStore,
                    billScanner = MlKitBillScanner(application)
                )
            }
        }
    }
}

enum class SplitMode(val label: String) {
    EQUAL("Equally"),
    EXACT("Amounts"),
    PERCENTAGE("Percent"),
    SHARES("Shares")
}

data class AddExpenseUiState(
    val amount: String = "",
    val description: String = "",
    val splitMode: SplitMode = SplitMode.EQUAL,
    val members: List<GroupMemberDto> = emptyList(),
    val includedMemberIds: Set<String> = emptySet(), // used by EQUAL
    val splitValues: Map<String, String> = emptyMap(), // userId -> typed value, used by the other modes
    val billUri: String? = null, // photo picked or taken, shown as a preview
    val billScanStatus: BillScanStatus? = null,
    val detectedAmount: String? = null, // total read off the bill, formatted like a typed amount
    val errorMessage: String? = null
)

enum class BillScanStatus { READING, FOUND_AMOUNT, NO_AMOUNT }

private fun AddExpenseUiState.valueOf(userId: String): Double =
    splitValues[userId]?.toDoubleOrNull() ?: 0.0

fun AddExpenseUiState.splitTotal(): Double = members.sumOf { valueOf(it.userId) }

// No members loaded (e.g. group not picked yet) means the backend splits equally between everyone
fun AddExpenseUiState.splitEntries(): List<SplitEntryDto> = when (splitMode) {
    SplitMode.EQUAL -> members.filter { it.userId in includedMemberIds }.map { SplitEntryDto(it.userId, null) }
    else -> members.map { SplitEntryDto(it.userId, valueOf(it.userId)) }
}

/** Mirrors the backend's checks so the user gets feedback before submitting. */
fun AddExpenseUiState.splitError(): String? {
    if (members.isEmpty()) return null
    val amount = amount.toDoubleOrNull() ?: 0.0
    if (members.any { (splitValues[it.userId]?.toDoubleOrNull() ?: 0.0) < 0 }) return "Values can't be negative"
    return when (splitMode) {
        SplitMode.EQUAL ->
            if (includedMemberIds.isEmpty()) "Select at least one person" else null
        SplitMode.EXACT ->
            if (abs(splitTotal() - amount) > 0.001) "Amounts add up to ${rupees(splitTotal())}, not ${rupees(amount)}" else null
        SplitMode.PERCENTAGE ->
            if (abs(splitTotal() - 100) > 0.001) "Percentages add up to %.2f%%, not 100%%".format(splitTotal()) else null
        SplitMode.SHARES ->
            if (splitTotal() <= 0) "Give at least one person a share" else null
    }
}

/** What a member will owe, for showing next to their input. */
fun AddExpenseUiState.previewShare(userId: String): Double {
    val amount = amount.toDoubleOrNull() ?: return 0.0
    return when (splitMode) {
        SplitMode.EQUAL ->
            if (userId in includedMemberIds && includedMemberIds.isNotEmpty()) amount / includedMemberIds.size else 0.0
        SplitMode.EXACT -> valueOf(userId)
        SplitMode.PERCENTAGE -> amount * valueOf(userId) / 100
        SplitMode.SHARES ->
            if (splitTotal() > 0) amount * valueOf(userId) / splitTotal() else 0.0
    }
}
