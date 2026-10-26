package com.example.splitease.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.splitease.bill.BillScanner
import com.example.splitease.bill.MlKitBillScanner
import com.example.splitease.data.AppDataStore
import com.example.splitease.data.Repository
import com.example.splitease.ui.AppApplication
import com.example.splitease.ui.RequestToasts
import com.example.splitease.ui.model.ActivitySummaryDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.collections.emptyList


enum class SortPreference(val displayName: String) {
    NEWEST("Newest First"),
    OLDEST("Oldest First"),
    HIGHEST("Highest Amount"),
    LOWEST("Lowest Amount")
}

class ActivityViewModel(
    private val repository: Repository,
    private val dataStore: AppDataStore,
    private val billScanner: BillScanner
): ViewModel() {

    // Cached app-wide; see AppDataStore
    val activityUiState: ActivitySummaryDto?
        get() = dataStore.activity

    val isRefreshing: Boolean
        get() = dataStore.isRefreshing

    init {
        dataStore.loadIfNeeded()
    }

    // Pull to refresh
    fun refresh() = dataStore.refresh()

    private val _sortPreference = MutableStateFlow(SortPreference.NEWEST)
    val sortPreference = _sortPreference.asStateFlow()
    private val _activityData = MutableStateFlow<ActivitySummaryDto?>(null)

    init {
        viewModelScope.launch{
            snapshotFlow { activityUiState }
                .collect { _activityData.value = it }
        }
    }


    val sortedExpenses = combine(_activityData, _sortPreference) { data: ActivitySummaryDto?, pref: SortPreference ->
        val list = data?.expenseHistory ?: emptyList()
        when (pref) {
            SortPreference.NEWEST -> list.sortedByDescending {
                it.date.let { d -> Instant.parse(d) } ?: Instant.MIN
            }
            SortPreference.OLDEST -> list.sortedBy {
                it.date.let { d -> Instant.parse(d) } ?: Instant.MAX
            }
            SortPreference.HIGHEST -> list.sortedByDescending { it.amount }
            SortPreference.LOWEST -> list.sortedBy { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSortPreference(displayName: String) {
        val pref = SortPreference.entries.find { it.displayName == displayName }
        if (pref != null) _sortPreference.value = pref
    }

    fun deleteExpense(expenseId: String, onSuccess:() -> Unit){
        viewModelScope.launch{
            try{
                val response = repository.deleteExpense(expenseId)
                if (!response.isSuccessful) {
                    RequestToasts.failure("Couldn't delete expense", response)
                    return@launch
                }
                println("Expense Delete: success")
                RequestToasts.success("Expense deleted")
                dataStore.refresh()
                onSuccess()
            }catch(e: Exception){
                println("Expense Delete: exception = ${e::class.java} ${e.message}")
                RequestToasts.failure("Couldn't delete expense", e)
            }
        }
    }

    // Expense whose bill photo is being uploaded or removed, for showing progress
    var billChangeInProgressFor by mutableStateOf<String?>(null)
        private set

    // Bumped when this user changes a bill, so the image cache doesn't show the old photo
    var billRevision by mutableIntStateOf(0)
        private set

    fun receiptUrl(expenseId: String, thumbnail: Boolean): String = repository.receiptUrl(expenseId, thumbnail)

    /** Attaches (or replaces) the bill photo of an expense the user paid for. */
    fun attachBill(expenseId: String, uri: String){
        if (billChangeInProgressFor != null) return
        billChangeInProgressFor = expenseId
        viewModelScope.launch {
            try {
                val bill = billScanner.scan(uri, readTotal = false)
                val response = repository.uploadReceipt(expenseId, bill.jpeg)
                if (!response.isSuccessful) {
                    RequestToasts.failure("Couldn't upload bill photo", response)
                    return@launch
                }
                RequestToasts.success("Bill photo attached")
                billRevision++
                dataStore.refresh()
            } catch (e: Exception) {
                println("Attach Bill: exception = ${e::class.java} ${e.message}")
                RequestToasts.failure("Couldn't upload bill photo", e)
            } finally {
                billChangeInProgressFor = null
            }
        }
    }

    fun removeBill(expenseId: String){
        if (billChangeInProgressFor != null) return
        billChangeInProgressFor = expenseId
        viewModelScope.launch {
            try {
                val response = repository.deleteReceipt(expenseId)
                if (!response.isSuccessful) {
                    RequestToasts.failure("Couldn't remove bill photo", response)
                    return@launch
                }
                RequestToasts.success("Bill photo removed")
                billRevision++
                dataStore.refresh()
            } catch (e: Exception) {
                println("Remove Bill: exception = ${e::class.java} ${e.message}")
                RequestToasts.failure("Couldn't remove bill photo", e)
            } finally {
                billChangeInProgressFor = null
            }
        }
    }

    companion object{
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as AppApplication)
                val repository = application.container.repository
                ActivityViewModel(
                    repository = repository,
                    dataStore = application.container.dataStore,
                    billScanner = MlKitBillScanner(application)
                )
            }
        }
    }
}
