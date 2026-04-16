# Encrypted Notes — Developer Guide

## Project Overview
A single-user Android application (Kotlin, minSdk 26) that stores AES-256-GCM encrypted text notes locally on the device. No network access, no cloud storage. Dark mode forced by default.

## Architecture
- **Pattern**: MVVM, multi-Activity (one Activity per screen)
- **Database**: Room (SQLite) — `encrypted_notes.db`
- **Crypto**: `javax.crypto` only — no third-party crypto libraries
- **Session**: In-memory key singleton (`AuthState`) + `SharedPreferences` timestamps

## Key Source Locations

| Area | Path |
|---|---|
| Encryption / key derivation | `app/src/main/java/com/hritikg952/encryptednotes/security/CryptoManager.kt` |
| In-memory session key | `app/src/main/java/com/hritikg952/encryptednotes/security/AuthState.kt` |
| Session timeout logic | `app/src/main/java/com/hritikg952/encryptednotes/security/SessionManager.kt` |
| Room database | `app/src/main/java/com/hritikg952/encryptednotes/data/db/AppDatabase.kt` |
| Note entity | `app/src/main/java/com/hritikg952/encryptednotes/data/model/Note.kt` |
| User entity | `app/src/main/java/com/hritikg952/encryptednotes/data/model/User.kt` |
| App entry point | `app/src/main/java/com/hritikg952/encryptednotes/ui/login/LoginActivity.kt` |

## Security Model

### Password storage
- `salt1` (32 random bytes) + `PBKDF2WithHmacSHA256(password, salt1, 120_000 iter, 256-bit)` stored as password hash
- Password is verified by re-deriving and comparing — raw password is never stored

### Note encryption key
- `salt2` (32 random bytes) stored in the `users` table
- Encryption key = `PBKDF2(password, salt2)` — derived fresh at each login
- Key lives only in `AuthState.encryptionKey` (memory), never written to disk
- On logout or session expiry: key bytes are zeroed before the reference is cleared

### Note storage
- Each note's `encryptedTitle` and `encryptedBody` are stored as `Base64(IV[12] || AES-GCM-ciphertext || tag[16])`
- Title and body use independent random IVs

### Session (auto-lock)
- Timeout: 5 minutes after the app goes to background
- `onPause` records the current time; `onResume` checks elapsed time
- If expired: `AuthState.logout()` → redirect to `LoginActivity`
- Applies to `NoteListActivity` and `NoteEditorActivity`

### Backup prevention
- `android:allowBackup="false"` + `dataExtractionRules.xml` excludes all domains
- The database is never uploaded to Google Drive or transferred to another device

## Build

```bash
# From project root
./gradlew assembleDebug        # build debug APK
./gradlew assembleRelease      # build release APK (requires signing config)
./gradlew installDebug         # build and install on connected device/emulator
```

Requirements: Android Studio Hedgehog or later, JDK 17+.

## Dependencies (see `gradle/libs.versions.toml`)

| Library | Purpose |
|---|---|
| Room 2.6.1 | Local SQLite database with DAO pattern |
| Lifecycle / ViewModel 2.8.2 | MVVM, survives config changes |
| Kotlin Coroutines 1.8.1 | Async DB and crypto operations on IO dispatcher |
| Material 3 1.12.0 | Minimal Material dark UI components |

No third-party crypto, networking, or analytics libraries.

## Screens

| Activity | Description |
|---|---|
| `LoginActivity` | Launcher. Routes to `SetupActivity` on first run, otherwise shows login form |
| `SetupActivity` | First-launch account creation (username + password + confirm) |
| `NoteListActivity` | RecyclerView of decrypted note titles + previews. FAB creates, long-press deletes, overflow menu logs out |
| `NoteEditorActivity` | Create or edit a single note. Launched with optional `EXTRA_NOTE_ID` |

## Common Tasks

### Adding a new field to notes
1. Add the encrypted field to `Note.kt`
2. Increment `AppDatabase` version and add a migration
3. Encrypt/decrypt the new field in `NoteEditorViewModel` and `NoteListViewModel`

### Changing the session timeout
Edit `SESSION_TIMEOUT_MS` in `SessionManager.kt`.

### Changing PBKDF2 iteration count
Edit `PBKDF2_ITERATIONS` in `CryptoManager.kt`. Note: changing this invalidates all existing accounts (stored hash was derived with the old count). Existing users will not be able to log in — only do this for fresh installs or with a migration strategy.
