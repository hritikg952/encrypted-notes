package com.hritikg952.encryptednotes.security

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

/**
 * All cryptographic operations for the app.
 *
 * Key derivation : PBKDF2WithHmacSHA256, 120 000 iterations, 256-bit output
 * Encryption     : AES-256-GCM, 12-byte random IV per operation
 * Storage format : Base64( IV[12] || ciphertext || GCM-tag[16] )
 *
 * Two separate salts are used per user account:
 *   salt1 → password verification hash  (stored, compared at login)
 *   salt2 → note encryption key         (derived fresh each login, held in memory only)
 */
object CryptoManager {

    private const val PBKDF2_ALGORITHM  = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS   = 256
    private const val SALT_LENGTH_BYTES = 32

    private const val AES_ALGORITHM     = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH    = 128   // bits
    private const val IV_LENGTH_BYTES   = 12    // 96-bit IV — NIST recommended for GCM

    // ── Salt ──────────────────────────────────────────────────────────────────

    fun generateSalt(): ByteArray =
        ByteArray(SALT_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }

    // ── PBKDF2 ────────────────────────────────────────────────────────────────

    /**
     * Derives a 256-bit byte array from [password] and [salt].
     * Used for password verification (salt1) and key derivation (salt2).
     * Must be called on a background thread — takes ~700 ms on mid-range hardware.
     */
    fun pbkdf2(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            KEY_LENGTH_BITS
        )
        return try {
            SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    // ── AES-256-GCM ───────────────────────────────────────────────────────────

    /**
     * Encrypts [plaintext] with [keyBytes] (32 bytes).
     * Returns Base64( IV[12] || ciphertext || tag[16] ).
     */
    fun encrypt(plaintext: String, keyBytes: ByteArray): String {
        val iv = ByteArray(IV_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(AES_ALGORITHM)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(keyBytes, "AES"),
            GCMParameterSpec(GCM_TAG_LENGTH, iv)
        )
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + ciphertext, Base64.NO_WRAP)
    }

    /**
     * Decrypts a value produced by [encrypt].
     * Throws if the key is wrong or data is tampered (GCM authentication failure).
     */
    fun decrypt(encoded: String, keyBytes: ByteArray): String {
        val combined   = Base64.decode(encoded, Base64.NO_WRAP)
        val iv         = combined.copyOfRange(0, IV_LENGTH_BYTES)
        val ciphertext = combined.copyOfRange(IV_LENGTH_BYTES, combined.size)
        val cipher     = Cipher.getInstance(AES_ALGORITHM)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(keyBytes, "AES"),
            GCMParameterSpec(GCM_TAG_LENGTH, iv)
        )
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    // ── Encoding helpers ──────────────────────────────────────────────────────

    fun toBase64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    fun fromBase64(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)
}
