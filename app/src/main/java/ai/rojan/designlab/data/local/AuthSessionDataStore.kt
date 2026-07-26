package ai.rojan.designlab.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * UX Refactor Phase 2: persists which [ai.rojan.designlab.domain.identity.PersonIdentity]
 * is currently logged in via the phone/OTP Identity Foundation, so a
 * returning customer's session survives a cold start instead of resetting
 * to logged-out every launch (see [ai.rojan.designlab.data.identity.DemoSessionProvider],
 * which is deliberately in-memory only). Separate DataStore file from
 * [roleDataStore] — this persists real identity, the other persists the
 * older, coarse Role selection; the two are intentionally kept apart, not
 * merged into one preferences file.
 */
val Context.authSessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_session_preferences")

/** Preference keys used by [ai.rojan.designlab.data.repository.AuthSessionRepositoryImpl]. */
internal object AuthSessionPreferencesKeys {
    val LOGGED_IN_PERSON_ID = stringPreferencesKey("logged_in_person_id")
}
