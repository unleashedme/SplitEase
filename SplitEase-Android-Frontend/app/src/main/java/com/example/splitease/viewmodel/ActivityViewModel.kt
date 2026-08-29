package com.example.splitease.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.splitease.data.Repository
import com.example.splitease.ui.AppApplication
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

class ActivityViewModel(private val repository: Repository): ViewModel() {


    var activityUiState by mutableStateOf<ActivitySummaryDto?>(null)
        private set

    init {
        getActivity()
    }

    fun getActivity(){
        viewModelScope.launch {
            try{
                activityUiState = repository.getActivity()
                println("Activity: getActivity success")
            }catch (e: Exception){
                println("Activity: exception = ${e::class.java} ${e.message}")
            }
        }
    }

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
                repository.deleteExpense(expenseId)
                println("Expense Delete: success")
                onSuccess()
            }catch(e: Exception){
                println("Expense Delete: exception = ${e::class.java} ${e.message}")
            }
        }
    }

    companion object{
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as AppApplication)
                val repository = application.container.repository
                ActivityViewModel(repository = repository)
            }
        }
    }
}
