package com.hritikg952.encryptednotes.ui.notes

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hritikg952.encryptednotes.EncryptedNotesApp
import com.hritikg952.encryptednotes.R
import com.hritikg952.encryptednotes.data.repository.NoteRepository
import com.hritikg952.encryptednotes.databinding.ActivityNoteListBinding
import com.hritikg952.encryptednotes.security.AuthState
import com.hritikg952.encryptednotes.security.SessionManager
import com.hritikg952.encryptednotes.ui.editor.NoteEditorActivity
import com.hritikg952.encryptednotes.ui.login.LoginActivity
import kotlinx.coroutines.launch

class NoteListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteListBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: NoteAdapter

    private val viewModel: NoteListViewModel by viewModels {
        val db = (application as EncryptedNotesApp).database
        NoteListViewModel.Factory(NoteRepository(db.noteDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)
        binding = ActivityNoteListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        adapter = NoteAdapter(
            onClick = { note ->
                startActivity(
                    Intent(this, NoteEditorActivity::class.java)
                        .putExtra(NoteEditorActivity.EXTRA_NOTE_ID, note.id)
                )
            },
            onLongClick = { note -> confirmDelete(note) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.fab.setOnClickListener {
            startActivity(Intent(this, NoteEditorActivity::class.java))
        }

        lifecycleScope.launch {
            viewModel.notes.collect { notes ->
                adapter.submitList(notes)
                binding.tvEmpty.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_note_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> { logout(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun confirmDelete(note: DecryptedNote) {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete_title)
            .setMessage(R.string.confirm_delete_message)
            .setPositiveButton(R.string.btn_delete) { _, _ -> viewModel.deleteNote(note.id) }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun logout() {
        AuthState.logout()
        sessionManager.clearSession()
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
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
