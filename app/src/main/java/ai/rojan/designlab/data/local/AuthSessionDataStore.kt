package ai.rojan.designlab.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * Persists which [ai.rojan.designlab.domain.identity.PersonIdentity] is
 * currently logged in via the phone/OTP Identity Foundation, so a
 * returning user's session survives a cold start instead of resetting to
 * logged-out every launch (see [ai.rojan.designlab.data.identity.DemoSessionProvider],
 * which is deliberately in-memory only).
 *
 * Customer Authentication Migration: persistence is unconditional — the
 * previous "Remember Me" preference key is removed; a logged-in session
 * always survives a cold start until an explicit logout, refresh-token
 * expiration, or security invalidation clears it.
 */
val Context.authSessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_session_preferences")

/** Preference keys used by [ai.rojan.designlab.data.repository.AuthSessionRepositoryImpl]. */
internal object AuthSessionPreferencesKeys {
    val LOGGED_IN_PERSON_ID = stringPreferencesKey("logged_in_person_id")
}
