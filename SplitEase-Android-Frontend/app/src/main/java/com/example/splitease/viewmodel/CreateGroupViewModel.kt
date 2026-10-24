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
import com.example.splitease.ui.RequestToasts
import kotlinx.coroutines.launch

class CreateGroupViewModel(private val repository: Repository): ViewModel() {

    var createGroupUiState by mutableStateOf(CreateGroupUiState())
        private set

    fun updateUiState(createGroupUiState: CreateGroupUiState){
        this.createGroupUiState = createGroupUiState
    }

    fun updateMemberEmail(index: Int, value: String) {
        val updated = createGroupUiState.members.toMutableList()
        updated[index] = value
        createGroupUiState = createGroupUiState.copy(members = updated)
    }

    fun addMember(){
        if (createGroupUiState.members.size > 3) return
        createGroupUiState = createGroupUiState.copy( members = createGroupUiState.members + "" )
    }

    fun resetUiState(){
        createGroupUiState = CreateGroupUiState()
    }

    fun createGroup(onSuccess: () -> Unit){
        if(createGroupUiState.members.size<=4){
            viewModelScope.launch {
                try {
                    val groupName = createGroupUiState.name
                    val response = repository.createGroup(
                        groupName = groupName,
                        members = createGroupUiState.members
                    )
                    if (!response.isSuccessful) {
                        RequestToasts.failure("Couldn't create group", response)
                        return@launch
                    }
                    RequestToasts.success("Group \"$groupName\" created")
                    onSuccess()
                    println("CREATE GROUP: name = ${createGroupUiState.name}, members = ${createGroupUiState.members}")
                } catch (e: Exception) {
                    println("CREATE GROUP: exception = ${e::class.java} ${e.message}")
                    RequestToasts.failure("Couldn't create group", e)
                }
            }
        }
    }


    companion object{
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as AppApplication)
                val repository = application.container.repository
                CreateGroupViewModel(repository = repository)
            }
        }
    }
}

data class CreateGroupUiState(
    val name: String = "",
    val members: List<String> = listOf("")
)