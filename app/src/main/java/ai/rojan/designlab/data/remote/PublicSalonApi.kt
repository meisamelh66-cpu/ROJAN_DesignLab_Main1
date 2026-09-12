package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import ai.rojan.designlab.data.remote.dto.PublicSalonResponseDto
import ai.rojan.designlab.data.remote.dto.PublicSalonSummaryResponseDto
import ai.rojan.designlab.data.remote.dto.PublicServiceCategoryResponseDto
import ai.rojan.designlab.data.remote.dto.PublicServiceResponseDto
import ai.rojan.designlab.data.remote.dto.PublicSpecialistResponseDto
import ai.rojan.designlab.data.remote.dto.TimeSlotResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit contract for the ROJAN backend's unauthenticated QR-code
 * customer journey (`ROJAN_Backend/PublicSalonController`,
 * `/api/v1/public/salons/{slug}/...`), resolved by a salon's public
 * `slug` rather than its id. Built on a plain, no-token [retrofit2.Retrofit]
 * instance (see `BackendApiContainer.buildPublicRetrofit`) — deliberately
 * not the authenticated one every other `*Api` in this package uses.
 */
interface PublicSalonApi {

    /**
     * The unauthenticated salon directory (`ROJAN_Backend` `PublicSalonDirectoryController`) —
     * the guest-browsable counterpart to [SalonApi.browseSalons]. Publicly
     * discoverable salons only, no bearer token required. The free-text filter
     * param is `search` here (not `name` as on the authenticated endpoint).
     */
    @GET("api/v1/public/salons")
    suspend fun browseSalons(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("search") search: String?,
        @Query("sortDirection") sortDirection: String,
    ): PagedResponseDto<PublicSalonSummaryResponseDto>

    @GET("api/v1/public/salons/{slug}")
    suspend fun getSalon(@Path("slug") slug: String): PublicSalonResponseDto

    @GET("api/v1/public/salons/{slug}/categories")
    suspend fun getCategories(@Path("slug") slug: String): List<PublicServiceCategoryResponseDto>

    @GET("api/v1/public/salons/{slug}/categories/{categoryId}/services")
    suspend fun getServices(
        @Path("slug") slug: String,
        @Path("categoryId") categoryId: String,
    ): List<PublicServiceResponseDto>

    @GET("api/v1/public/salons/{slug}/specialists")
    suspend fun getSpecialists(@Path("slug") slug: String): List<PublicSpecialistResponseDto>

    @GET("api/v1/public/salons/{slug}/specialists/{specialistId}/available-slots")
    suspend fun getAvailableSlots(
        @Path("slug") slug: String,
        @Path("specialistId") specialistId: String,
        @Query("serviceId") serviceId: String,
        @Query("date") date: String,
        @Query("slotIntervalMinutes") slotIntervalMinutes: Int,
    ): List<TimeSlotResponseDto>
}
