package com.cristianv.radianceapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristianv.radianceapp.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            android.util.Log.d("AuthViewModel", "Login called for: $email")
            val result = repository.login(email, password)
            android.util.Log.d("AuthViewModel", "Login result: ${result.isSuccess}, error: ${result.exceptionOrNull()?.message}")
            _authState.value = if (result.isSuccess) {
                AuthState.Success
            } else {
                AuthState.Error(
                    result.exceptionOrNull()?.message ?: "Login failed"
                )
            }
        }
    }

    fun signUp(email: String, password: String, fullName: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            android.util.Log.d("AuthViewModel", "SignUp called for: $email")
            val result = repository.signUp(email, password, fullName)
            android.util.Log.d("AuthViewModel", "SignUp result: ${result.isSuccess}, error: ${result.exceptionOrNull()?.message}")
            _authState.value = if (result.isSuccess) {
                AuthState.Success
            } else {
                AuthState.Error(
                    result.exceptionOrNull()?.message ?: "Sign up failed"
                )
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            android.util.Log.d("AuthViewModel", "Google Sign In called with idToken (length=${idToken.length})")
            val result = repository.signInWithGoogle(idToken)
            android.util.Log.d("AuthViewModel", "Google Sign In result: isSuccess=${result.isSuccess}, error=${result.exceptionOrNull()?.message}")
            _authState.value = if (result.isSuccess) {
                android.util.Log.d("AuthViewModel", "Google Sign In SUCCESS — user: ${result.getOrNull()?.email}")
                AuthState.Success
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Google sign in failed"
                android.util.Log.e("AuthViewModel", "Google Sign In FAILED: $errorMsg")
                AuthState.Error(errorMsg)
            }
        }
    }

    /** Called from the UI when a Google Sign In error is detected before calling the ViewModel flow. */
    fun setError(message: String) {
        android.util.Log.e("AuthViewModel", "setError: $message")
        _authState.value = AuthState.Error(message)
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            repository.resetPassword(email)
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun logout() {
        repository.logout()
        _authState.value = AuthState.Idle
    }
}
