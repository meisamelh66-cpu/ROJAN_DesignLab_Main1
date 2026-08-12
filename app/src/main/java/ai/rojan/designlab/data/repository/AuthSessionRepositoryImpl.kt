package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.local.AuthSessionPreferencesKeys
import ai.rojan.designlab.domain.repository.AuthSessionRepository
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * [AuthSessionRepository] backed by Preferences DataStore.
 *
 * Any non-cancellation read failure recovers to "no one logged in" rather
 * than propagating and permanently stalling a collector — the same class
 * of physical-device failure [ai.rojan.designlab.presentation.session.SessionViewModel]'s
 * doc comment describes guarding against.
 */
class AuthSessionRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : AuthSessionRepository {

    override suspend fun savePersonId(personId: String) {
        dataStore.edit { preferences ->
            preferences[AuthSessionPreferencesKeys.LOGGED_IN_PERSON_ID] = personId
        }
    }

    override suspend fun clearPersonId() {
        dataStore.edit { preferences ->
            preferences.remove(AuthSessionPreferencesKeys.LOGGED_IN_PERSON_ID)
        }
    }

    /** Session persistence is unconditional — every successful login survives a cold start, until an explicit [clearPersonId] (logout, expired/revoked refresh token). */
    override fun observePersonId(): Flow<String?> =
        dataStore.data
            .catch { throwable ->
                if (throwable is CancellationException) {
                    throw throwable
                }
                emit(emptyPreferences())
            }
            .map { preferences -> preferences[AuthSessionPreferencesKeys.LOGGED_IN_PERSON_ID] }
}
