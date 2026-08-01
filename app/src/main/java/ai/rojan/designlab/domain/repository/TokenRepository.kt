package ai.rojan.designlab.domain.repository

/**
 * Persists the JWT access/refresh token pair issued by the ROJAN backend.
 * Abstracts the storage mechanism (encrypted local storage, in the current
 * implementation) away from callers, per Clean Architecture.
 *
 * Synchronous by design, unlike [AuthSessionRepository]'s suspend/Flow
 * shape — [okhttp3.Interceptor] and [okhttp3.Authenticator] (the two
 * callers that matter most here) are synchronous SPIs, and the underlying
 * storage read/write is fast enough not to need to be async.
 */
interface TokenRepository {

    /** Persists [accessToken]/[refreshToken] as the current pair, replacing any previous values. */
    fun saveTokens(accessToken: String, refreshToken: String)

    /** Clears any persisted tokens (e.g. on logout or an unrecoverable refresh failure). */
    fun clearTokens()

    /** The currently persisted access token, or `null` if none is stored. */
    fun accessToken(): String?

    /** The currently persisted refresh token, or `null` if none is stored. */
    fun refreshToken(): String?
}
