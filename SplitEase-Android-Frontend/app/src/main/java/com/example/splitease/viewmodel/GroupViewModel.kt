package com.example.splitease.viewmodel


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.splitease.data.Repository
import com.example.splitease.ui.AppApplication
import com.example.splitease.ui.model.GroupScreenDataResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


enum class GroupSortOrder(val displayName: String) {
    MOST_RECENT("Most Recent"),
    NAME_AZ("Name(A-Z)"),
    MOST_MEMBERS("Most Members"),
    HIGHEST_EXPENSES("Highest Expenses")
}

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

    private val _groupSortOrder = MutableStateFlow(GroupSortOrder.MOST_RECENT)
    val groupSortOrder = _groupSortOrder.asStateFlow()
    private val _groupUiData = MutableStateFlow<GroupScreenDataResponse?>(null)

    init {
        viewModelScope.launch {
            snapshotFlow { groupUiState }
                .collect { _groupUiData.value = it }
        }
    }

    val sortedGroupList = combine(_groupUiData, _groupSortOrder){ data,pref ->
        val list = data?.groups?:emptyList()
        when(pref){
            GroupSortOrder.NAME_AZ -> list.sortedBy { it.groupName.lowercase() }
            GroupSortOrder.MOST_MEMBERS -> list.sortedByDescending { it.memberCount }
            GroupSortOrder.HIGHEST_EXPENSES -> list.sortedByDescending { it.totalGroupExpense }
            GroupSortOrder.MOST_RECENT -> list.sortedByDescending { g ->
                g.expenses.mapNotNull { it.date }.maxOrNull() ?: ""
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSortOrder(displayName: String){
        val pref = GroupSortOrder.entries.find { it.displayName == displayName }
        if(pref != null) _groupSortOrder.value = pref
    }

    fun deleteGroup(groupId: String, onSuccess:() -> Unit){
        viewModelScope.launch {
            try {
                repository.deleteGroup(groupId = groupId)
                println("GroupDelete: deletingGroupData success")
                onSuccess()
            }catch (e: Exception){
                println("GroupDelete: exception = ${e::class.java} ${e.message}")
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