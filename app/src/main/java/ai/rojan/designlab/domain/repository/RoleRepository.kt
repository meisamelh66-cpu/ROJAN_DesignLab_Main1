package ai.rojan.designlab.domain.repository

import ai.rojan.designlab.domain.model.Role
import kotlinx.coroutines.flow.Flow

/**
 * Persists and restores the user's selected [Role].
 *
 * Abstracts the storage mechanism (Preferences DataStore, in the current
 * implementation) away from the use-case and presentation layers, per
 * Clean Architecture — nothing above this interface should know or care
 * that DataStore specifically is involved.
 */
interface RoleRepository {

    /** Persists [role] as the user's selected role, replacing any previous value. */
    suspend fun saveSelectedRole(role: Role)

    /**
     * Emits the currently persisted role (`null` if none has been
     * selected yet), and re-emits whenever it changes.
     */
    fun observeSelectedRole(): Flow<Role?>
}
