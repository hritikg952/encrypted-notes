package com.hritikg952.encryptednotes.ui.editor

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
import kotlinx.coroutines.withContext

sealed class EditorUiState {
    object Idle : EditorUiState()
    object Loading : EditorUiState()
    object Saved : EditorUiState()
    object SessionExpired : EditorUiState()
    data class NoteLoaded(val title: String, val body: String) : EditorUiState()
    data class Error(val message: String) : EditorUiState()
}

class NoteEditorViewModel(private val noteRepository: NoteRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<EditorUiState>(EditorUiState.Idle)
    val uiState: StateFlow<EditorUiState> = _uiState

    private var currentNoteId: Long? = null

    fun loadNote(noteId: Long) {
        currentNoteId = noteId
        viewModelScope.launch(Dispatchers.IO) {
            val key = AuthState.encryptionKey
            if (key == null) {
                withContext(Dispatchers.Main) { _uiState.value = EditorUiState.SessionExpired }
                return@launch
            }
            val note = noteRepository.getNoteById(noteId)
            if (note == null) {
                withContext(Dispatchers.Main) {
                    _uiState.value = EditorUiState.Error("Note not found")
                }
                return@launch
            }
            try {
                val title = CryptoManager.decrypt(note.encryptedTitle, key)
                val body  = CryptoManager.decrypt(note.encryptedBody, key)
                withContext(Dispatchers.Main) {
                    _uiState.value = EditorUiState.NoteLoaded(title, body)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = EditorUiState.Error("Failed to decrypt note")
                }
            }
        }
    }

    fun saveNote(title: String, body: String) {
        if (title.isBlank()) {
            _uiState.value = EditorUiState.Error("Title cannot be empty")
            return
        }
        _uiState.value = EditorUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val key = AuthState.encryptionKey
            if (key == null) {
                withContext(Dispatchers.Main) { _uiState.value = EditorUiState.SessionExpired }
                return@launch
            }
            try {
                val encTitle = CryptoManager.encrypt(title.trim(), key)
                val encBody  = CryptoManager.encrypt(body, key)
                val now      = System.currentTimeMillis()

                val id = currentNoteId
                if (id == null) {
                    noteRepository.insert(
                        Note(encryptedTitle = encTitle, encryptedBody = encBody,
                             createdAt = now, updatedAt = now)
                    )
                } else {
                    val existing = noteRepository.getNoteById(id)
                    if (existing != null) {
                        noteRepository.update(
                            existing.copy(encryptedTitle = encTitle,
                                          encryptedBody  = encBody,
                                          updatedAt      = now)
                        )
                    }
                }
                withContext(Dispatchers.Main) { _uiState.value = EditorUiState.Saved }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = EditorUiState.Error("Failed to save note")
                }
            }
        }
    }

    class Factory(private val noteRepository: NoteRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NoteEditorViewModel(noteRepository) as T
    }
}
