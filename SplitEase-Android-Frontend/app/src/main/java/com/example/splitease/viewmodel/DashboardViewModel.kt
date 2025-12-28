package com.example.splitease.viewmodel

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
import com.example.splitease.ui.model.DashboardStatResponse
import kotlinx.coroutines.launch

class DashboardViewModel(private val repository: Repository): ViewModel() {

    var dashboardUiState by mutableStateOf<DashboardStatResponse?>(null)
        private set

    init {
        getDashboardStat()
    }

    fun getDashboardStat(){
        viewModelScope.launch {
            try {
                dashboardUiState = repository.getDashboardStat()
                println("Dashboard: getDashboardStat success")
            }catch (e: Exception){
                println("Dashboard: exception = ${e::class.java} ${e.message}")
            }
        }
    }

    companion object{
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as AppApplication)
                val repository = application.container.repository
                DashboardViewModel(repository = repository)
            }
        }
    }
}

