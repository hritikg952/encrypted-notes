package com.hritikg952.encryptednotes.ui.setup

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hritikg952.encryptednotes.EncryptedNotesApp
import com.hritikg952.encryptednotes.data.repository.UserRepository
import com.hritikg952.encryptednotes.databinding.ActivitySetupBinding
import com.hritikg952.encryptednotes.security.SessionManager
import com.hritikg952.encryptednotes.ui.notes.NoteListActivity
import kotlinx.coroutines.launch

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding

    private val viewModel: SetupViewModel by viewModels {
        val db = (application as EncryptedNotesApp).database
        SetupViewModel.Factory(
            UserRepository(db.userDao()),
            SessionManager(this)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCreate.setOnClickListener {
            viewModel.createAccount(
                binding.etUsername.text.toString(),
                binding.etPassword.text.toString(),
                binding.etConfirmPassword.text.toString()
            )
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is SetupUiState.Idle -> Unit
                    is SetupUiState.Loading -> showLoading(true)
                    is SetupUiState.Success -> {
                        showLoading(false)
                        goToNoteList()
                    }
                    is SetupUiState.Error -> {
                        showLoading(false)
                        showError(state.message)
                    }
                }
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnCreate.isEnabled = !loading
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
