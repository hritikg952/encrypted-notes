# Encrypted Notes

A single-user Android app that stores AES-256-GCM encrypted text notes entirely on your device. No internet connection, no cloud storage, no tracking.

## Features

- **End-to-end local encryption** — notes are encrypted with AES-256-GCM before being written to the database
- **Password-protected access** — PBKDF2 key derivation (120,000 iterations); raw password is never stored
- **Auto-lock** — app locks itself after 5 minutes in the background
- **Full CRUD** — create, read, edit, and delete notes (title + body)
- **Offline-only** — no network permission, works without internet
- **Dark mode** — forced dark theme, minimal UI, no animations
- **Backup-safe** — `allowBackup="false"` prevents notes from leaking via Google Drive backup

## Install on your phone (no computer needed)

1. Go to the [Releases](../../releases) page of this repository
2. Tap the latest release and download **app-debug.apk**
3. Open the downloaded file on your phone
4. If prompted, allow **"Install unknown apps"** for your browser or file manager
5. Tap **Install**

A new APK is automatically built and published to Releases on every merge to `main`.

## Build from source

Requirements: Android Studio Hedgehog (or later), JDK 17+

```bash
git clone https://github.com/hritikg952/encrypted-notes.git
cd encrypted-notes
gradle assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

> **Note:** No `gradlew` wrapper is committed to the repo. Use Android Studio (**Build > Make Project**) or install Gradle 8.7 manually and run `gradle assembleDebug`.

## Security overview

| Layer | Mechanism |
|---|---|
| Password storage | PBKDF2WithHmacSHA256 — hash + `salt1` stored, password never saved |
| Note encryption key | Derived via PBKDF2 from password + `salt2`; lives in memory only, never persisted |
| Note storage | `Base64(IV[12] \|\| AES-GCM-ciphertext \|\| tag[16])` per field |
| Session | 5-minute inactivity timeout; key bytes zeroed on logout/expiry |
| Backup | `allowBackup="false"` + `dataExtractionRules.xml` blocks all backup domains |

## Screens

| Screen | Description |
|---|---|
| Setup | First-launch only — create your username and password |
| Login | Enter your password to unlock; routes to Setup on first run |
| Note list | All notes (title + body preview); FAB to create, long-press to delete, overflow menu to log out |
| Note editor | Edit title and body; save via toolbar button |

## Tech stack

- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Architecture**: MVVM, multi-Activity
- **Database**: Room (SQLite)
- **Crypto**: `javax.crypto` only — no third-party libraries
- **UI**: Material 3 Dark, ViewBinding
