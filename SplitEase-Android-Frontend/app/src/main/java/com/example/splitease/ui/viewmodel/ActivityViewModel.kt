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
import com.example.splitease.ui.model.ActivitySummaryDto
import kotlinx.coroutines.launch

class ActivityViewModel(private val repository: Repository): ViewModel() {


    var activityUiState by mutableStateOf<ActivitySummaryDto?>(null)
        private set

    init {
        println("DEBUG: ActivityViewModel Created! Fetching data...")
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
