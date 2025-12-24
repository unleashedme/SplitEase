package com.example.splitease.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.splitease.data.Repository
import com.example.splitease.dto.SettlementDto
import com.example.splitease.ui.AppApplication
import com.example.splitease.ui.model.SettlementSummary
import kotlinx.coroutines.launch

class SettlementViewModel(private val repository: Repository): ViewModel(){

    var settlements by mutableStateOf<List<SettlementSummary>>(emptyList())
        private set

    fun loadSettlements() {
        viewModelScope.launch {
            try{
                settlements = repository.getMyPayables()
                println("SettlementList: loadSettlements success")
            }
            catch(e : Exception){
                println("SettlementList: exception = ${e::class.java} ${e.message}")
            }
        }
    }

    fun recordPayment(groupId: String, toUserId: String, amount: Double, note: String = "", onSuccess:() -> Unit){
        viewModelScope.launch{
            try{
                repository.recordSettlement(
                    SettlementDto(
                        groupId = groupId,
                        toUserId = toUserId,
                        amount = amount,
                        note = note,
                    )
                )
                onSuccess()
                println("Settlement: recordPayment success")
            }catch (e: Exception){
                println("Settlement: exception = ${e::class.java} ${e.message}")
            }
        }
    }

    companion object{
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as AppApplication)
                val repository = application.container.repository
                SettlementViewModel(repository = repository)
            }
        }
    }
}
