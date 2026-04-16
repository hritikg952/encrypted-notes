package com.hritikg952.encryptednotes.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hritikg952.encryptednotes.EncryptedNotesApp
import com.hritikg952.encryptednotes.data.repository.UserRepository
import com.hritikg952.encryptednotes.databinding.ActivityLoginBinding
import com.hritikg952.encryptednotes.security.AuthState
import com.hritikg952.encryptednotes.security.SessionManager
import com.hritikg952.encryptednotes.ui.notes.NoteListActivity
import com.hritikg952.encryptednotes.R
import com.hritikg952.encryptednotes.ui.setup.SetupActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    private val viewModel: LoginViewModel by viewModels {
        val db = (application as EncryptedNotesApp).database
        LoginViewModel.Factory(
            UserRepository(db.userDao()),
            SessionManager(this)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        // Route to Setup on first launch
        if (!sessionManager.isSetupDone) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnUnlock.setOnClickListener { attemptLogin() }
        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { attemptLogin(); true } else false
        }

        lifecycleScope.launch {
            viewModel.username.collect { name ->
                if (name.isNotEmpty()) {
                    binding.tvUsername.text = getString(R.string.label_username) + ": $name"
                }
            }
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is LoginUiState.Idle -> Unit
                    is LoginUiState.Loading -> showLoading(true)
                    is LoginUiState.Success -> {
                        showLoading(false)
                        goToNoteList()
                    }
                    is LoginUiState.Error -> {
                        showLoading(false)
                        showError(state.message)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // If already logged in and session is still valid, skip login screen
        if (AuthState.isLoggedIn() && !sessionManager.isSessionExpired()) {
            goToNoteList()
        }
    }

    private fun attemptLogin() {
        viewModel.login(binding.etPassword.text.toString())
    }

    private fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnUnlock.isEnabled = !loading
        binding.tvError.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun goToNoteList() {
        startActivity(
            Intent(this, NoteListActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }
}
