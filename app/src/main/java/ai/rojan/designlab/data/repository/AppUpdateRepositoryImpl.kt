package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.AppReleaseApi
import ai.rojan.designlab.data.remote.dto.AppReleaseLatestResponseDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.domain.repository.AppUpdateInfo
import ai.rojan.designlab.domain.repository.AppUpdateRepository

/**
 * [applicationId]/[currentVersionCode] are this exact running app's own real
 * identity — resolved once from `BuildConfig.APPLICATION_ID`/`BuildConfig.VERSION_CODE`
 * at DI-construction time (see `BackendApiContainer`), never passed by a
 * caller, never hardcoded to one flavor. Every network/parsing failure
 * (offline, timeout, a non-2xx response, a malformed body) already becomes a
 * typed [Result.failure] via [safeApiCall] — this class adds no extra
 * try/catch of its own; the caller (`AppUpdateViewModel`) is the one that
 * decides to fail open on any failure at all, not this layer.
 */
class AppUpdateRepositoryImpl(
    private val appReleaseApi: AppReleaseApi,
    private val applicationId: String,
    private val currentVersionCode: Int,
) : AppUpdateRepository {

    override suspend fun checkForUpdate(): Result<AppUpdateInfo> =
        safeApiCall { appReleaseApi.getLatestRelease(applicationId, currentVersionCode) }
            .map { it.toDomain() }

    private fun AppReleaseLatestResponseDto.toDomain() = AppUpdateInfo(
        updateAvailable = updateAvailable,
        forceUpdate = forceUpdate,
        latestVersionName = latestVersion,
        latestVersionCode = latestVersionCode,
        downloadUrl = downloadUrl,
        sha256 = sha256,
        fileSizeBytes = fileSizeBytes,
        releaseNotes = releaseNotes,
        releaseDate = releaseDate,
    )
}
