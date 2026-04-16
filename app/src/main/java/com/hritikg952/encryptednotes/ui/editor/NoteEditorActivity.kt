package com.hritikg952.encryptednotes.ui.editor

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hritikg952.encryptednotes.EncryptedNotesApp
import com.hritikg952.encryptednotes.R
import com.hritikg952.encryptednotes.data.repository.NoteRepository
import com.hritikg952.encryptednotes.databinding.ActivityNoteEditorBinding
import com.hritikg952.encryptednotes.security.AuthState
import com.hritikg952.encryptednotes.security.SessionManager
import com.hritikg952.encryptednotes.ui.login.LoginActivity
import kotlinx.coroutines.launch

class NoteEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
    }

    private lateinit var binding: ActivityNoteEditorBinding
    private lateinit var sessionManager: SessionManager

    private val viewModel: NoteEditorViewModel by viewModels {
        val db = (application as EncryptedNotesApp).database
        NoteEditorViewModel.Factory(NoteRepository(db.noteDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L).takeIf { it != -1L }

        if (noteId != null) {
            binding.toolbar.title = getString(R.string.editor_title_edit)
            viewModel.loadNote(noteId)
        } else {
            binding.toolbar.title = getString(R.string.editor_title_new)
        }

        binding.btnSave.setOnClickListener {
            viewModel.saveNote(
                binding.etTitle.text.toString(),
                binding.etBody.text.toString()
            )
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is EditorUiState.Idle -> Unit
                    is EditorUiState.Loading -> showLoading(true)
                    is EditorUiState.NoteLoaded -> {
                        showLoading(false)
                        binding.etTitle.setText(state.title)
                        binding.etBody.setText(state.body)
                        // Move cursor to end of body
                        binding.etBody.setSelection(binding.etBody.text?.length ?: 0)
                    }
                    is EditorUiState.Saved -> {
                        showLoading(false)
                        finish()
                    }
                    is EditorUiState.SessionExpired -> {
                        showLoading(false)
                        lock()
                    }
                    is EditorUiState.Error -> {
                        showLoading(false)
                        showError(state.message)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!AuthState.isLoggedIn() || sessionManager.isSessionExpired()) {
            lock()
        } else {
            sessionManager.recordActivity()
        }
    }

    override fun onPause() {
        super.onPause()
        sessionManager.recordActivity()
    }

    private fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !loading
        binding.tvError.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun lock() {
        AuthState.logout()
        sessionManager.clearSession()
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }
}
