package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.local.RolePreferencesKeys
import ai.rojan.designlab.domain.model.Role
import ai.rojan.designlab.domain.repository.RoleRepository
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * [RoleRepository] backed by Preferences DataStore.
 *
 * The selected role is stored as its enum [Role.name] (a plain string),
 * which keeps the stored value stable even if enum declaration order
 * changes later. An unrecognized or corrupted stored value is treated as
 * "no role selected" rather than crashing the app.
 */
class RoleRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : RoleRepository {

    override suspend fun saveSelectedRole(role: Role) {
        dataStore.edit { preferences ->
            preferences[RolePreferencesKeys.SELECTED_ROLE] = role.name
            // Post Build UX Flow Audit: set together, atomically, in the
            // same edit transaction - see RolePreferencesKeys.ONBOARDING_COMPLETED's
            // doc comment for why this exists.
            preferences[RolePreferencesKeys.ONBOARDING_COMPLETED] = true
        }
    }

    override fun observeSelectedRole(): Flow<Role?> =
        dataStore.data
            .catch { throwable ->
                // Production fix: previously only IOException was recovered
                // here, which meant any other read failure (e.g. an
                // OEM-specific storage/security exception seen on some
                // physical devices but not in the emulator) propagated
                // uncaught, permanently killing the collecting Flow and
                // leaving SessionViewModel's restoreState stuck on
                // Loading forever. CancellationException is explicitly
                // re-thrown — it must never be swallowed, since it's a
                // structured-concurrency control signal, not a real
                // error. Every other throwable is treated the same way
                // IOException always was: recover to "no preferences yet"
                // (the safe fallback described below in RoleRepository's
                // contract) rather than dying silently.
                if (throwable is kotlinx.coroutines.CancellationException) {
                    throw throwable
                }
                emit(emptyPreferences())
            }
            .map { preferences ->
                // Post Build UX Flow Audit: require BOTH keys together -
                // a role value alone, without the explicit completion
                // flag, is treated as "no role" (routes to Welcome, not
                // a dashboard). See ONBOARDING_COMPLETED's doc comment.
                val onboardingCompleted = preferences[RolePreferencesKeys.ONBOARDING_COMPLETED] == true
                if (!onboardingCompleted) {
                    null
                } else {
                    preferences[RolePreferencesKeys.SELECTED_ROLE]?.let { storedName ->
                        runCatching { Role.valueOf(storedName) }.getOrNull()
                    }
                }
            }
}
