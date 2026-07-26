package ai.rojan.designlab.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Persists and restores which person is currently logged in via the
 * phone/OTP Identity Foundation.
 *
 * Abstracts the storage mechanism (Preferences DataStore, in the current
 * implementation) away from the presentation layer, per Clean
 * Architecture — nothing above this interface should know or care that
 * DataStore specifically is involved. Mirrors [RoleRepository]'s shape
 * for the same reason that repository exists, but is deliberately a
 * separate interface/store — see [ai.rojan.designlab.data.local.authSessionDataStore]'s
 * doc comment for why the two aren't merged.
 */
interface AuthSessionRepository {

    /** Persists [personId] as the currently logged-in person, replacing any previous value. */
    suspend fun savePersonId(personId: String)

    /** Clears the persisted logged-in person (e.g. on logout). */
    suspend fun clearPersonId()

    /**
     * Emits the currently persisted logged-in person id (`null` if no one
     * is logged in), and re-emits whenever it changes.
     */
    fun observePersonId(): Flow<String?>
}
