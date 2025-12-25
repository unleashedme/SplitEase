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
import com.example.splitease.datastore.AuthStore
import com.example.splitease.ui.AppApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ProfileState{
    object LoggedIn: ProfileState()
    object LoggedOut: ProfileState()
}

class ProfileViewModel(private val authStore: AuthStore, private val application: AppApplication): ViewModel() {


    val profileUiState: StateFlow<ProfileUiState> =
        authStore.profileUiStateFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5),
                initialValue = ProfileUiState()
            )

    var profileState: ProfileState by mutableStateOf(ProfileState.LoggedIn)
        private set

    private val _navigateToLoginScreen = MutableSharedFlow<Unit>()
    val navigateToLoginScreen = _navigateToLoginScreen.asSharedFlow()

    fun logOut(){
        viewModelScope.launch(Dispatchers.IO) {
            authStore.clear()
            application.resetContainer()
            _navigateToLoginScreen.emit(Unit)
        }
    }

    companion object{
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as AppApplication)
                val authStore = application.authStore
                ProfileViewModel(authStore = authStore, application = application)
            }
        }
    }
}

data class ProfileUiState(
    val name: String? = "",
    val email: String? = "",
    val phone: String? = "",
    val upiId: String? = ""
)