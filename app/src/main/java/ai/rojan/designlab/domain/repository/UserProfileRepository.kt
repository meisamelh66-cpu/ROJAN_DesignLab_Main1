package ai.rojan.designlab.domain.repository

/**
 * Customer Profile Personalization Phase 5B — the customer editing their own
 * avatar and profile-cover image via the backend's `/api/v1/users/me/media/(avatar|cover)`
 * endpoints (Phase 5A). Every call acts on the authenticated caller's own
 * account; there is no user id to pass. Each returns the refreshed
 * [AuthenticatedUser] (with the new `avatarUrl` / `coverUrl` resolved), which
 * the caller pushes back into [ai.rojan.designlab.presentation.auth.AuthViewModel].
 *
 * The bytes are already downscaled + JPEG-re-encoded client-side (shared
 * `ui/media/ImageDownscale.kt` pipeline) before they reach here.
 */
interface UserProfileRepository {

    suspend fun uploadAvatar(fileBytes: ByteArray, fileName: String, mimeType: String): Result<AuthenticatedUser>

    suspend fun uploadCover(fileBytes: ByteArray, fileName: String, mimeType: String): Result<AuthenticatedUser>

    /** Idempotent on the backend — removing an already-empty slot is a successful no-op. */
    suspend fun removeAvatar(): Result<AuthenticatedUser>

    suspend fun removeCover(): Result<AuthenticatedUser>
}
