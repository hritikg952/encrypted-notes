package com.hritikg952.encryptednotes.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores an encrypted note. Both title and body are encrypted individually using AES-256-GCM.
 * Each field is stored as Base64( IV (12 bytes) || ciphertext || GCM-tag (16 bytes) ).
 */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val encryptedTitle: String,
    val encryptedBody: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
