package com.hritikg952.encryptednotes.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hritikg952.encryptednotes.data.model.Note
import com.hritikg952.encryptednotes.data.repository.NoteRepository
import com.hritikg952.encryptednotes.security.AuthState
import com.hritikg952.encryptednotes.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NoteListViewModel(private val noteRepository: NoteRepository) : ViewModel() {

    private val _notes = MutableStateFlow<List<DecryptedNote>>(emptyList())
    val notes: StateFlow<List<DecryptedNote>> = _notes

    init {
        observeNotes()
    }

    private fun observeNotes() {
        viewModelScope.launch(Dispatchers.IO) {
            noteRepository.getAllNotes().collect { encryptedNotes ->
                val key = AuthState.encryptionKey
                if (key == null) {
                    _notes.value = emptyList()
                    return@collect
                }
                _notes.value = encryptedNotes.mapNotNull { note -> decrypt(note, key) }
            }
        }
    }

    private fun decrypt(note: Note, key: ByteArray): DecryptedNote? {
        return try {
            val title   = CryptoManager.decrypt(note.encryptedTitle, key)
            val preview = CryptoManager.decrypt(note.encryptedBody, key)
                .take(120)
                .replace('\n', ' ')
            DecryptedNote(note.id, title, preview, note.updatedAt)
        } catch (e: Exception) {
            null   // tampered or corrupt entry — skip silently
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val note = noteRepository.getNoteById(noteId) ?: return@launch
            noteRepository.delete(note)
        }
    }

    class Factory(private val noteRepository: NoteRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NoteListViewModel(noteRepository) as T
    }
}
