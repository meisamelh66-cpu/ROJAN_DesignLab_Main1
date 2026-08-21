package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.MediaAssetResponseDto
import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import ai.rojan.designlab.data.remote.dto.SalonResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit contract for the ROJAN backend's Salon API (`ROJAN_Backend/API_CONTRACT.md`). */
interface SalonApi {

    @GET("api/v1/salons")
    suspend fun browseSalons(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("name") name: String?,
        @Query("sortDirection") sortDirection: String,
    ): PagedResponseDto<SalonResponseDto>

    @GET("api/v1/salons/{salonId}")
    suspend fun getSalon(@Path("salonId") salonId: String): SalonResponseDto

    /**
     * Media Sprint P0: reuses the exact same, already-shipped
     * `GET /api/v1/salons/{salonId}/media` contract
     * [ai.rojan.designlab.data.remote.ManagerMediaApi] calls for Manager -
     * not a new backend endpoint, just a second Retrofit client for the
     * same authenticated (non-owner-gated) read.
     */
    @GET("api/v1/salons/{salonId}/media")
    suspend fun getMedia(
        @Path("salonId") salonId: String,
        @Query("mediaType") mediaType: String? = null,
    ): List<MediaAssetResponseDto>
}
