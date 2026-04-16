package com.hritikg952.encryptednotes.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores account credentials. Single user only — PK is always 1.
 *
 * passwordHash : Base64( PBKDF2(password, salt1) ) — used to verify the password at login
 * salt1        : Base64( random 32 bytes ) — salt for password verification hash
 * salt2        : Base64( random 32 bytes ) — salt for deriving the note encryption key
 *
 * The actual encryption key is NEVER persisted; it is derived at login and kept in memory only.
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: Int = 1,
    val username: String,
    val passwordHash: String,
    val salt1: String,
    val salt2: String
)
