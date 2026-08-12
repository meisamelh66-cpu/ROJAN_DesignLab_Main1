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
 *
 * Customer Authentication Migration: session persistence is unconditional
 * — a successful OTP verification always survives a cold start; the
 * previous "Remember Me" concept (an opt-in toggle on top of this) is
 * removed. Re-authentication is required only on explicit logout
 * ([clearPersonId]), refresh-token expiration, or a security invalidation
 * (both surfaced as [clearPersonId] calls from the token-refresh layer).
 */
interface AuthSessionRepository {

    /** Persists [personId] as the currently logged-in person, replacing any previous value. */
    suspend fun savePersonId(personId: String)

    /** Clears the persisted logged-in person (e.g. on logout, or a failed session restore). */
    suspend fun clearPersonId()

    /**
     * Emits the currently persisted logged-in person id (`null` if no one
     * is logged in), and re-emits whenever it changes.
     */
    fun observePersonId(): Flow<String?>
}
