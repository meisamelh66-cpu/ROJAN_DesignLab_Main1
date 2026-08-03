package ai.rojan.designlab.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Persists and restores which person is currently logged in via the
 * phone/OTP Identity Foundation.
 *
 * Abstracts the storage mechanism (Preferences DataStore, in the current
 * implementation) away from the presentation layer, per Clean
 * Architecture — nothing above this interface should know or care that
 * DataStore specifically is involved.
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

    /** Persists the "Remember Me" choice made at login. */
    suspend fun saveRememberMe(remember: Boolean)

    /** Whether the current session should survive a cold start — defaults to `true` when never explicitly set, so this is additive and never regresses a pre-existing session. */
    fun observeRememberMe(): Flow<Boolean>
}
