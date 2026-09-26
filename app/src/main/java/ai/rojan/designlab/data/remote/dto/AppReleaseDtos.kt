package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Wire shape of `GET /api/v1/public/app-releases/{applicationId}/latest`
 * (`ROJAN_Backend`'s `PublicAppReleaseController`/`PublicLatestReleaseResponse`)
 * — confirmed by direct source read against the real, already-shipped Phase 1
 * contract. Deliberately mirrors that response exactly: `updateAvailable`/
 * `forceUpdate`/`latestVersionCode`/`downloadUrl` are computed server-side
 * from the real release data and this caller's own `versionCode` query
 * param — the client never recomputes any of these fields itself, only maps
 * this DTO into [ai.rojan.designlab.domain.repository.AppUpdateInfo].
 */
@Serializable
data class AppReleaseLatestResponseDto(
    val updateAvailable: Boolean,
    val forceUpdate: Boolean,
    val latestVersion: String,
    val latestVersionCode: Int,
    val downloadUrl: String,
    val sha256: String,
    val fileSizeBytes: Long,
    val releaseNotes: String? = null,
    val releaseDate: String,
)
