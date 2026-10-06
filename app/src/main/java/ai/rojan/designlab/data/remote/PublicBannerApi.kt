package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.BannerResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit contract for the ROJAN backend's real, unauthenticated banner
 * read surface (`ROJAN_Backend` `PublicBannerController`,
 * `/api/v1/public/banners`) — the same existing endpoint every other
 * `BannerTarget` (SITE/CUSTOMER/DESKTOP) already uses, now also valid for
 * `target=MANAGER` (`V43__manager_banner_target.sql`). Built on a plain,
 * no-token [retrofit2.Retrofit] instance (see `BackendApiContainer`'s
 * `buildPlainRetrofit()`), same reasoning as [PublicSalonApi].
 */
interface PublicBannerApi {

    /** Active banners for one target only, pre-sorted by display order — inactive banners are never returned here. */
    @GET("api/v1/public/banners")
    suspend fun getActiveBanners(@Query("target") target: String): List<BannerResponseDto>
}
