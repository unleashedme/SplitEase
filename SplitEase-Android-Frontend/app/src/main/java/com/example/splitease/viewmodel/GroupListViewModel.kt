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
import com.example.splitease.ui.model.UserGroupResponse
import kotlinx.coroutines.launch

class GroupListViewModel(private val repository: Repository): ViewModel(){

    var groupListUiState by mutableStateOf(GroupListUiState())
        private set

    init {
        getGroupList()
    }
    fun getGroupList(){
        viewModelScope.launch {
            try {
                val groups = repository.getMyGroups()
                groupListUiState = groupListUiState.copy(groups = groups)
                println("GROUP LIST called is Successfully")
            }catch(e: Exception){
                println("GROUP LIST: exception = ${e::class.java} ${e.message}")
            }
        }
    }

    companion object{
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as AppApplication)
                val repository = application.container.repository
                GroupListViewModel(repository = repository)
            }
        }
    }
}

data class GroupListUiState(
    val groups: List<UserGroupResponse> = emptyList()
)