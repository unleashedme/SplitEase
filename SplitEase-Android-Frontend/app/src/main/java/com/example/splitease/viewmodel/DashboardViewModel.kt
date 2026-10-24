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
import com.example.splitease.data.AppDataStore
import com.example.splitease.data.Repository
import com.example.splitease.ui.AppApplication
import com.example.splitease.ui.RequestToasts
import com.example.splitease.ui.model.DashboardStatResponse
import kotlinx.coroutines.launch

class DashboardViewModel(private val dataStore: AppDataStore): ViewModel() {

    // Cached app-wide; see AppDataStore
    val dashboardUiState: DashboardStatResponse?
        get() = dataStore.dashboardStats

    val isRefreshing: Boolean
        get() = dataStore.isRefreshing

    init {
        dataStore.loadIfNeeded()
    }

    // Pull to refresh
    fun refresh() = dataStore.refresh()

    companion object{
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as AppApplication)
                DashboardViewModel(dataStore = application.container.dataStore)
            }
        }
    }
}

