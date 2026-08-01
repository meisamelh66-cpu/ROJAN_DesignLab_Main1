package ai.rojan.designlab.domain.repository

/** Domain-facing shape of a user account as returned by the ROJAN backend. */
data class AuthenticatedUser(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String,
)

/**
 * Talks to the ROJAN backend's auth API (register/login/refresh/me — see
 * `ROJAN_Backend/API.md`). A successful [login] persists the returned
 * tokens via [TokenRepository] as a side effect; callers never handle raw
 * tokens directly. Token refresh on a 401 happens transparently inside the
 * networking layer (see `data/remote/TokenAuthenticator.kt`), not through
 * this interface.
 */
interface BackendAuthRepository {

    /** Registers a new customer account. Does not log the user in or persist any tokens. */
    suspend fun register(email: String, password: String, fullName: String): Result<AuthenticatedUser>

    /** Authenticates and persists the returned access/refresh token pair via [TokenRepository]. */
    suspend fun login(email: String, password: String): Result<AuthenticatedUser>

    /** Fetches the account behind the currently stored access token. */
    suspend fun currentUser(): Result<AuthenticatedUser>
}
