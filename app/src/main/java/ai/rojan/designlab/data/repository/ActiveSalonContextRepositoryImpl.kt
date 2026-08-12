package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.local.ActiveSalonPreferencesKeys
import ai.rojan.designlab.domain.repository.ActiveSalonContextRepository
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * [ActiveSalonContextRepository] backed by Preferences DataStore - same
 * shape and resilience as [AuthSessionRepositoryImpl]: a non-cancellation
 * read failure recovers to "no salon selected" rather than propagating and
 * stalling a collector.
 */
class ActiveSalonContextRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : ActiveSalonContextRepository {

    override suspend fun saveActiveSalonId(salonId: String) {
        dataStore.edit { preferences ->
            preferences[ActiveSalonPreferencesKeys.ACTIVE_SALON_ID] = salonId
        }
    }

    override suspend fun clearActiveSalonId() {
        dataStore.edit { preferences ->
            preferences.remove(ActiveSalonPreferencesKeys.ACTIVE_SALON_ID)
        }
    }

    override fun observeActiveSalonId(): Flow<String?> =
        dataStore.data
            .catch { throwable ->
                if (throwable is CancellationException) {
                    throw throwable
                }
                emit(emptyPreferences())
            }
            .map { preferences -> preferences[ActiveSalonPreferencesKeys.ACTIVE_SALON_ID] }
}
