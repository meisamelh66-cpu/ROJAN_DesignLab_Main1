package ai.rojan.designlab.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * Persists which salon id the current user has set as active (Active Salon
 * Context & Selection Flow) - a separate DataStore file from
 * [authSessionDataStore], since this is a distinct concern (which salon,
 * not who's logged in) that must survive independently: cleared on logout
 * same as the session, but never conflated with it.
 */
val Context.activeSalonDataStore: DataStore<Preferences> by preferencesDataStore(name = "active_salon_preferences")

/** Preference keys used by [ai.rojan.designlab.data.repository.ActiveSalonContextRepositoryImpl]. */
internal object ActiveSalonPreferencesKeys {
    val ACTIVE_SALON_ID = stringPreferencesKey("active_salon_id")
}
