package com.hritikg952.encryptednotes.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hritikg952.encryptednotes.data.repository.UserRepository
import com.hritikg952.encryptednotes.security.AuthState
import com.hritikg952.encryptednotes.security.CryptoManager
import com.hritikg952.encryptednotes.security.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username

    init {
        loadUsername()
    }

    private fun loadUsername() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userRepository.getUser()
            withContext(Dispatchers.Main) {
                _username.value = user?.username ?: ""
            }
        }
    }

    fun login(password: String) {
        if (password.isBlank()) {
            _uiState.value = LoginUiState.Error("Please enter your password")
            return
        }
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val user = userRepository.getUser()
            if (user == null) {
                withContext(Dispatchers.Main) {
                    _uiState.value = LoginUiState.Error("No account found")
                }
                return@launch
            }

            val salt1      = CryptoManager.fromBase64(user.salt1)
            val salt2      = CryptoManager.fromBase64(user.salt2)
            val inputHash  = CryptoManager.pbkdf2(password, salt1)
            val storedHash = CryptoManager.fromBase64(user.passwordHash)

            if (!inputHash.contentEquals(storedHash)) {
                withContext(Dispatchers.Main) {
                    _uiState.value = LoginUiState.Error("Invalid username or password")
                }
                return@launch
            }

            val encKey = CryptoManager.pbkdf2(password, salt2)
            AuthState.setKey(encKey)
            sessionManager.recordActivity()

            withContext(Dispatchers.Main) {
                _uiState.value = LoginUiState.Success
            }
        }
    }

    class Factory(
        private val userRepository: UserRepository,
        private val sessionManager: SessionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LoginViewModel(userRepository, sessionManager) as T
    }
}
