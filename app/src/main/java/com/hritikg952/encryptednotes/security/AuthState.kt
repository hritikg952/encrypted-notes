package com.hritikg952.encryptednotes.security

/**
 * Process-level singleton that holds the in-memory encryption key.
 *
 * The key is derived from the user's password at login and kept here for the
 * duration of the session. It is NEVER written to disk.
 *
 * When the session expires or the user logs out, [logout] is called which
 * overwrites the key bytes with zeros before clearing the reference, preventing
 * the plaintext key from lingering in memory.
 */
object AuthState {

    @Volatile
    var encryptionKey: ByteArray? = null
        private set

    fun isLoggedIn(): Boolean = encryptionKey != null

    fun setKey(key: ByteArray) {
        encryptionKey = key
    }

    fun logout() {
        encryptionKey?.fill(0)
        encryptionKey = null
    }
}
