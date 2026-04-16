package com.hritikg952.encryptednotes.data.repository

import com.hritikg952.encryptednotes.data.db.NoteDao
import com.hritikg952.encryptednotes.data.model.Note
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)

    suspend fun insert(note: Note): Long = noteDao.insert(note)

    suspend fun update(note: Note) = noteDao.update(note)

    suspend fun delete(note: Note) = noteDao.delete(note)
}
