package com.hritikg952.encryptednotes.security

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages non-sensitive session metadata in SharedPreferences:
 *   - whether the first-time account setup has been completed
 *   - the timestamp of the last known foreground activity
 *
 * The 5-minute auto-lock is enforced by checking elapsed time since the app
 * last reported activity. The actual encryption key lives in [AuthState].
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME           = "session_prefs"
        private const val KEY_SETUP_DONE       = "setup_done"
        private const val KEY_LAST_ACTIVE_TIME = "last_active_time"
        private const val SESSION_TIMEOUT_MS   = 5L * 60 * 1000   // 5 minutes
    }

    // ── Setup flag ────────────────────────────────────────────────────────────

    var isSetupDone: Boolean
        get() = prefs.getBoolean(KEY_SETUP_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_DONE, value).apply()

    // ── Activity tracking ─────────────────────────────────────────────────────

    /** Call whenever the app enters the foreground or the user authenticates. */
    fun recordActivity() {
        prefs.edit().putLong(KEY_LAST_ACTIVE_TIME, System.currentTimeMillis()).apply()
    }

    /**
     * Returns true if more than 5 minutes have elapsed since the last recorded activity.
     * Returns false (not expired) if no timestamp has been stored yet — meaning the user
     * just logged in and hasn't paused the app.
     */
    fun isSessionExpired(): Boolean {
        val last = prefs.getLong(KEY_LAST_ACTIVE_TIME, 0L)
        if (last == 0L) return false
        return System.currentTimeMillis() - last > SESSION_TIMEOUT_MS
    }

    /** Clears the activity timestamp so the timer does not carry over after logout. */
    fun clearSession() {
        prefs.edit().remove(KEY_LAST_ACTIVE_TIME).apply()
    }
}
