package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.BannerResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit contract for the ROJAN backend's unauthenticated banner surface
 * (`ROJAN_Backend/PublicBannerController`, `GET /api/v1/public/banners`) —
 * built on a plain, no-token [retrofit2.Retrofit] instance (see
 * `BackendApiContainer.buildPlainRetrofit`), same as [PublicSalonApi].
 * Only active banners for the given [target] are ever returned, already
 * sorted by display order server-side — nothing for the client to filter
 * or sort.
 */
interface PublicBannerApi {
    @GET("api/v1/public/banners")
    suspend fun getActiveBanners(@Query("target") target: String = "MANAGER"): List<BannerResponseDto>
}
