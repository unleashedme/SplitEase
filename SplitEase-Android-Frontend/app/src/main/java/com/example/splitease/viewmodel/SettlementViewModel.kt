package com.example.splitease.viewmodel

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
import com.example.splitease.data.AppDataStore
import com.example.splitease.data.Repository
import com.example.splitease.dto.SettlementDto
import com.example.splitease.ui.AppApplication
import com.example.splitease.ui.RequestToasts
import com.example.splitease.ui.model.SettlementSummary
import kotlinx.coroutines.launch

class SettlementViewModel(
    private val repository: Repository,
    private val dataStore: AppDataStore
): ViewModel(){

    // Cached app-wide; see AppDataStore
    val settlements: List<SettlementSummary>
        get() = dataStore.payables

    // An empty list while this is true means "not loaded yet", not "nothing to settle"
    val isRefreshing: Boolean
        get() = dataStore.isRefreshing

    init {
        dataStore.loadIfNeeded()
    }

    fun recordPayment(toUserId: String, amount: Double, note: String = "", onSuccess:() -> Unit){
        viewModelScope.launch{
            try{
                repository.recordSettlement(
                    SettlementDto(
                        toUserId = toUserId,
                        amount = amount,
                        note = note,
                    )
                )
                RequestToasts.success("Payment of ${rupees(amount)} recorded")
                dataStore.refresh()
                onSuccess()
                println("Settlement: recordPayment success")
            }catch (e: Exception){
                println("Settlement: exception = ${e::class.java} ${e.message}")
                RequestToasts.failure("Couldn't record payment", e)
            }
        }
    }

    companion object{
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as AppApplication)
                val repository = application.container.repository
                SettlementViewModel(repository = repository, dataStore = application.container.dataStore)
            }
        }
    }
}
