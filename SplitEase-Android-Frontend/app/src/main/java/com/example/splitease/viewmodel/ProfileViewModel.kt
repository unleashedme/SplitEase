package com.example.splitease.viewmodel

import android.util.Log
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
import com.example.splitease.datastore.AuthStore
import com.example.splitease.ui.AppApplication
import com.google.firebase.messaging.FirebaseMessaging
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

class ProfileViewModel(private val authStore: AuthStore,
                       private val application: AppApplication,
                       private val repository: Repository): ViewModel() {


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
            profileState = ProfileState.LoggedOut
            authStore.clear()
            application.resetContainer()
            _navigateToLoginScreen.emit(Unit)
        }
    }

    fun updateNotificationPreference(isEnabled: Boolean){
        viewModelScope.launch{
            try {
                authStore.saveNotificationPreference(isEnabled)
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        viewModelScope.launch {
                            repository.updateFcmToken(token)
                        }
                    } else {
                        viewModelScope.launch{
                            repository.updateFcmToken("")
                        }

                        FirebaseMessaging.getInstance().deleteToken()
                    }
                    Log.d("ProfileVM", "Notification preference synced: $isEnabled")
                }
            } catch (e: Exception) {
                Log.e("ProfileVM", "Failed to sync preference: ${e.message}")
            }
        }
    }

    companion object{
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as AppApplication)
                val authStore = application.authStore
                val repository = application.container.repository
                ProfileViewModel(authStore = authStore, application = application, repository = repository)
            }
        }
    }
}

data class ProfileUiState(
    val name: String? = "",
    val email: String? = "",
    val phone: String? = "",
    val isNotificationsEnabled: Boolean = true
)