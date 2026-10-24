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
import com.example.splitease.ui.model.UserGroupResponse
import kotlinx.coroutines.launch

class GroupListViewModel(private val dataStore: AppDataStore): ViewModel(){

    // Cached app-wide; see AppDataStore
    val groupListUiState: GroupListUiState
        get() = GroupListUiState(groups = dataStore.myGroups)

    init {
        dataStore.loadIfNeeded()
    }

    companion object{
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as AppApplication)
                GroupListViewModel(dataStore = application.container.dataStore)
            }
        }
    }
}

data class GroupListUiState(
    val groups: List<UserGroupResponse> = emptyList()
)