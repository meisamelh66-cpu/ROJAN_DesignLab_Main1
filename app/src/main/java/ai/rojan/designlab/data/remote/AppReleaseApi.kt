package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.AppReleaseLatestResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit contract for the ROJAN backend's public "check for update"
 * surface (`ROJAN_Backend`'s `PublicAppReleaseController`) — no
 * authentication, by design; every Android client (Manager/Customer/
 * Reception) calls this before the user is necessarily logged in at all.
 * `applicationId` is always this exact running app's own real
 * `BuildConfig.APPLICATION_ID` (see `AppUpdateRepositoryImpl`, which bakes
 * it in at construction time) — never a hardcoded or cross-flavor value.
 */
interface AppReleaseApi {

    @GET("api/v1/public/app-releases/{applicationId}/latest")
    suspend fun getLatestRelease(
        @Path("applicationId") applicationId: String,
        @Query("versionCode") versionCode: Int,
    ): AppReleaseLatestResponseDto
}
