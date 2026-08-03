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

    /**
     * "Remember Me" enforcement lives here, not in any caller: when the
     * last session wasn't marked to be remembered, this emits `null`
     * regardless of what's still stored under [AuthSessionPreferencesKeys.LOGGED_IN_PERSON_ID] —
     * an unremembered session simply never resurfaces on the next cold
     * start, without [ai.rojan.designlab.presentation.session.SessionViewModel]
     * or [ai.rojan.designlab.presentation.auth.AuthViewModel] needing to
     * know "Remember Me" exists at all.
     */
    override fun observePersonId(): Flow<String?> =
        dataStore.data
            .catch { throwable ->
                if (throwable is CancellationException) {
                    throw throwable
                }
                emit(emptyPreferences())
            }
            .map { preferences ->
                val rememberMe = preferences[AuthSessionPreferencesKeys.REMEMBER_ME] ?: true
                if (rememberMe) preferences[AuthSessionPreferencesKeys.LOGGED_IN_PERSON_ID] else null
            }

    override suspend fun saveRememberMe(remember: Boolean) {
        dataStore.edit { preferences ->
            preferences[AuthSessionPreferencesKeys.REMEMBER_ME] = remember
        }
    }

    override fun observeRememberMe(): Flow<Boolean> =
        dataStore.data
            .catch { throwable ->
                if (throwable is CancellationException) {
                    throw throwable
                }
                emit(emptyPreferences())
            }
            .map { preferences -> preferences[AuthSessionPreferencesKeys.REMEMBER_ME] ?: true }
}
