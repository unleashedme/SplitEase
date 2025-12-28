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
import com.example.splitease.ui.AppApplication
import kotlinx.coroutines.launch

class AddExpenseViewModel(private val repository : Repository) : ViewModel() {

    var addExpenseUiState by mutableStateOf(AddExpenseUiState())
        private set

    fun updateUiState(addExpenseUiState: AddExpenseUiState){
        this.addExpenseUiState = addExpenseUiState
    }

    fun resetUiState(){
        addExpenseUiState = AddExpenseUiState()
    }

    fun addExpense(groupId: String, onSuccess:() -> Unit){
        if(groupId.isBlank() || addExpenseUiState.description.isBlank()
            || addExpenseUiState.amount.toDouble() <= 0.0) return
        viewModelScope.launch {
            try {
                repository.addExpense(
                    groupId = groupId,
                    amount = addExpenseUiState.amount.toDouble(),
                    description = addExpenseUiState.description
                )
                println("Add Expense called is Successfully")
                onSuccess()
            }catch (e: Exception){
                println("ADD EXPENSE: exception = ${e::class.java} ${e.message}")
            }
        }
    }

    companion object{
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as AppApplication)
                val repository = application.container.repository
                AddExpenseViewModel(repository = repository)
            }
        }
    }
}

data class AddExpenseUiState(
    val amount: String = "",
    val description: String = ""
)