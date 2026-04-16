package com.hritikg952.encryptednotes

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.hritikg952.encryptednotes.data.db.AppDatabase

class EncryptedNotesApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        // Force dark mode regardless of system setting
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}
