package com.hritikg952.encryptednotes.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hritikg952.encryptednotes.data.model.User
import com.hritikg952.encryptednotes.data.repository.UserRepository
import com.hritikg952.encryptednotes.security.AuthState
import com.hritikg952.encryptednotes.security.CryptoManager
import com.hritikg952.encryptednotes.security.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class SetupUiState {
    object Idle : SetupUiState()
    object Loading : SetupUiState()
    object Success : SetupUiState()
    data class Error(val message: String) : SetupUiState()
}

class SetupViewModel(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<SetupUiState>(SetupUiState.Idle)
    val uiState: StateFlow<SetupUiState> = _uiState

    fun createAccount(username: String, password: String, confirmPassword: String) {
        when {
            username.isBlank() || password.isBlank() || confirmPassword.isBlank() ->
                _uiState.value = SetupUiState.Error("Please fill in all fields")
            username.trim().length < 3 ->
                _uiState.value = SetupUiState.Error("Username must be at least 3 characters")
            password.length < 6 ->
                _uiState.value = SetupUiState.Error("Password must be at least 6 characters")
            password != confirmPassword ->
                _uiState.value = SetupUiState.Error("Passwords do not match")
            else -> doCreate(username.trim(), password)
        }
    }

    private fun doCreate(username: String, password: String) {
        _uiState.value = SetupUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val salt1        = CryptoManager.generateSalt()
            val salt2        = CryptoManager.generateSalt()
            val passwordHash = CryptoManager.pbkdf2(password, salt1)
            val encKey       = CryptoManager.pbkdf2(password, salt2)

            val user = User(
                username     = username,
                passwordHash = CryptoManager.toBase64(passwordHash),
                salt1        = CryptoManager.toBase64(salt1),
                salt2        = CryptoManager.toBase64(salt2)
            )
            userRepository.insertUser(user)

            AuthState.setKey(encKey)
            sessionManager.isSetupDone = true
            sessionManager.recordActivity()

            withContext(Dispatchers.Main) {
                _uiState.value = SetupUiState.Success
            }
        }
    }

    class Factory(
        private val userRepository: UserRepository,
        private val sessionManager: SessionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SetupViewModel(userRepository, sessionManager) as T
    }
}
