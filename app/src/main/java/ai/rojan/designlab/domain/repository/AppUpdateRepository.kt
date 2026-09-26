package ai.rojan.designlab.domain.repository

/**
 * Real update-check result from the backend's public app-releases endpoint
 * (`ROJAN_Backend`'s `PublicAppReleaseController`). The server is the sole
 * authority for [updateAvailable]/[forceUpdate]/[latestVersionCode]/
 * [downloadUrl] — this app never recomputes any of these fields itself, only
 * decides which UI to show for a given combination (see
 * [ai.rojan.designlab.presentation.update.AppUpdateViewModel]). [sha256]/
 * [fileSizeBytes] are preserved from the real response for future
 * artifact-integrity verification, even though nothing in this app checks
 * them yet — no client-side checksum mechanism is invented here.
 */
data class AppUpdateInfo(
    val updateAvailable: Boolean,
    val forceUpdate: Boolean,
    val latestVersionName: String,
    val latestVersionCode: Int,
    val downloadUrl: String,
    val sha256: String,
    val fileSizeBytes: Long,
    val releaseNotes: String?,
    val releaseDate: String,
)

/**
 * Checks this exact running app (its own real `applicationId`/installed
 * `versionCode`, baked into the implementation at construction time — see
 * [ai.rojan.designlab.data.repository.AppUpdateRepositoryImpl]) against the
 * real backend for a newer release. Never takes an applicationId or
 * versionCode parameter here — there is exactly one meaningful answer for
 * "am I up to date", and callers must never be able to ask about a
 * different app.
 */
interface AppUpdateRepository {
    suspend fun checkForUpdate(): Result<AppUpdateInfo>
}
