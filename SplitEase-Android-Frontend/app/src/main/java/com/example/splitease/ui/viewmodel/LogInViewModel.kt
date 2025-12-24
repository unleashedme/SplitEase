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
import com.example.splitease.datastore.AuthStore
import com.example.splitease.ui.AppApplication
import com.example.splitease.ui.model.LogInResponse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch


sealed class LoginState {
    object Idle : LoginState()
    data class Success(val response: LogInResponse): LoginState()
    object Loading : LoginState()
    data class Error(val message: String) : LoginState()
}
class LogInViewModel(private val repository: Repository, private val authStore: AuthStore): ViewModel(){

    var logInUiState by mutableStateOf(LogInUiState())
        private set

    var loginState: LoginState by mutableStateOf(LoginState.Idle)
        private set


    private val _navigateToDashboard = MutableSharedFlow<Unit>()
    val navigateToDashboard = _navigateToDashboard.asSharedFlow()


    fun updateUiState(logInDetails: LogInDetails){
        logInUiState = LogInUiState(logInDetails = logInDetails, isEntryValid = validateLogInInput(logInDetails))
    }

    private fun validateLogInInput(uiState: LogInDetails = logInUiState.logInDetails): Boolean{
        with(uiState){
            return email.isNotBlank() && password.isNotBlank() && email.endsWith("@gmail.com")
        }
    }


    fun logIn(){
        if(validateLogInInput()){
            viewModelScope.launch{
                loginState = LoginState.Loading
                try{

                    val result = repository.login(
                        logInUiState.logInDetails.email,
                        logInUiState.logInDetails.password
                    )

                    authStore.save(
                        result.token,
                        result.name,
                        result.email,
                        result.phone,
                        result.upiId
                    )
                    _navigateToDashboard.emit(Unit)
                    loginState = LoginState.Success(result)

                }catch (e: Exception){
                    println("LOGIN: exception = ${e::class.java} ${e.message}")
                    loginState = LoginState.Error(e.message ?: "error")
                }
            }
        }
    }

    companion object{
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as AppApplication)
                val authRepository = application.container.repository
                val authStore = application.authStore
                LogInViewModel(repository = authRepository, authStore = authStore)
            }
        }
    }
}


data class LogInUiState(
    val logInDetails: LogInDetails = LogInDetails(),
    val isEntryValid: Boolean = false
)

data class LogInDetails(
    val email: String = "",
    val password: String = ""
)