package ai.rojan.designlab.domain.repository

/**
 * Domain-facing shape of a user account as returned by the ROJAN backend.
 * [email]/[phoneNumber] are each independently nullable, mirroring the
 * backend account model - an email/password account has no phone, a
 * phone-only OTP account has no email. A non-null [phoneNumber] is always
 * verified: the backend's only path to setting it is completing real OTP
 * verification (`User.registerWithPhone`) - there is no unverified-phone
 * state to distinguish.
 */
data class AuthenticatedUser(
    val id: String,
    val email: String?,
    val phoneNumber: String? = null,
    val fullName: String,
    val role: String,
)

/** Domain-facing result of a successful OTP request/resend — mirrors the backend's `OtpIssuedResponse`. */
data class OtpIssued(
    val phoneNumber: String,
    val expiresInSeconds: Long,
    val canResendAfterSeconds: Long,
)

/**
 * Talks to the ROJAN backend's auth API (register/login/refresh/me/otp — see
 * `ROJAN_Backend/API.md`). A successful [login]/[verifyOtp] persists the
 * returned tokens via [TokenRepository] as a side effect; callers never
 * handle raw tokens directly. Token refresh on a 401 happens transparently
 * inside the networking layer (see `data/remote/TokenAuthenticator.kt`), not
 * through this interface.
 */
interface BackendAuthRepository {

    /** Registers a new customer account. Does not log the user in or persist any tokens. */
    suspend fun register(email: String, password: String, fullName: String): Result<AuthenticatedUser>

    /** Authenticates and persists the returned access/refresh token pair via [TokenRepository]. */
    suspend fun login(email: String, password: String): Result<AuthenticatedUser>

    /** Fetches the account behind the currently stored access token. */
    suspend fun currentUser(): Result<AuthenticatedUser>

    /**
     * OTP Authentication Entry Flow Integration: requests (or resends) an
     * OTP code for [phoneNumber] via the backend's already-existing
     * `POST /api/v1/auth/otp/request`. Does not persist anything — no
     * session exists yet until [verifyOtp] succeeds.
     */
    suspend fun requestOtp(phoneNumber: String): Result<OtpIssued>

    /**
     * Verifies a previously-requested OTP [code] for [phoneNumber] via the
     * backend's already-existing `POST /api/v1/auth/otp/verify`, and
     * persists the returned access/refresh token pair via [TokenRepository]
     * on success — same side-effect shape as [login]. [fullName] is used by
     * the backend only the first time this phone number completes
     * verification (new-account creation) and silently ignored for an
     * existing account.
     */
    suspend fun verifyOtp(phoneNumber: String, code: String, fullName: String? = null): Result<AuthenticatedUser>
}
