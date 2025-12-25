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
import com.example.splitease.ui.model.GroupScreenDataResponse
import kotlinx.coroutines.launch

class GroupViewModel(private val repository: Repository): ViewModel(){

    var groupUiState by mutableStateOf<GroupScreenDataResponse?>(null)
        private set

    init {
        getGroupScreenData()
    }

    fun getGroupScreenData(){
        viewModelScope.launch {
            try{
                groupUiState = repository.getGroupScreenData()
                println("Group: getGroupScreenData success")
            }catch (e: Exception){
                println("Group: exception = ${e::class.java} ${e.message}")
            }
        }
    }

    companion object{
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as AppApplication)
                val repository = application.container.repository
                GroupViewModel(repository = repository)
            }
        }
    }
}