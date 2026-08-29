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
import com.example.splitease.dto.UserDto
import com.example.splitease.ui.AppApplication
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed class RegisterState{
    object Idle: RegisterState()
    data class Success(val response: String): RegisterState()
    object Loading: RegisterState()
    data class Error(val message: String): RegisterState()
}

class RegisterViewModel(private val repository: Repository): ViewModel() {

    var registerUiState by mutableStateOf(RegisterUiState())
        private set

    var registerState: RegisterState by mutableStateOf(RegisterState.Idle)
        private set

    fun updateUiState(userDetails: UserDetails){
        registerUiState = RegisterUiState(userDetails = userDetails, isEntryValid = validateRegisterInput(userDetails))
    }

    private val _navigateToLoginScreen = MutableSharedFlow<Unit>()
    val navigateToLoginScreen = _navigateToLoginScreen.asSharedFlow()

    private fun validateRegisterInput(uiState: UserDetails = registerUiState.userDetails): Boolean{
        with(uiState){
            return name.isNotBlank() && email.isNotBlank() && mobileNumber.isNotBlank() && password.isNotBlank()
                    && password == confirmPassword && email.endsWith("@gmail.com") && mobileNumber.length == 10
        }
    }

    fun register(){
        if(validateRegisterInput()){
            viewModelScope.launch{
                registerState = RegisterState.Loading
                try{
                    println("Register: before api call")

                    repository.register(
                        UserDto(
                            name = registerUiState.userDetails.name,
                            email = registerUiState.userDetails.email,
                            phone = registerUiState.userDetails.mobileNumber,
                            password = registerUiState.userDetails.password
                        )
                    )
                    println("Register: after api call")
                    _navigateToLoginScreen.emit(Unit)
                    registerState = RegisterState.Success(response = "Registered Successfully")
                }catch(e: Exception){
                    println("REGISTER: exception = ${e::class.java} ${e.message}")
                    registerState = RegisterState.Error(e.message ?: "error")
                }
            }
        }
    }

    companion object{
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as AppApplication)
                val repository = application.container.repository
                RegisterViewModel(repository = repository)
            }
        }
    }
}

data class RegisterUiState(
    val userDetails: UserDetails = UserDetails(),
    val isEntryValid: Boolean = false
)

data class UserDetails(
    val name: String = "",
    val email: String = "",
    val mobileNumber: String = "",
    val password: String = "",
    val confirmPassword: String = ""
)