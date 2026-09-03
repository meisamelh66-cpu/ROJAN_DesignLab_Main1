package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.CreateSpecialistRequestDto
import ai.rojan.designlab.data.remote.dto.MediaAssetResponseDto
import ai.rojan.designlab.data.remote.dto.SpecialistResponseDto
import ai.rojan.designlab.data.remote.dto.UpdateSpecialistRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit contract for the ROJAN backend's Specialist API (`ROJAN_Backend/API_CONTRACT.md`). `createSpecialist`/`updateSpecialist` are owner-only. */
interface SpecialistApi {

    @GET("api/v1/salons/{salonId}/specialists")
    suspend fun getSpecialists(@Path("salonId") salonId: String): List<SpecialistResponseDto>

    @GET("api/v1/salons/{salonId}/specialists/{specialistId}")
    suspend fun getSpecialist(
        @Path("salonId") salonId: String,
        @Path("specialistId") specialistId: String,
    ): SpecialistResponseDto

    /** Media System Evolution v2 - reuses the same authenticated `GET .../media` read [ai.rojan.designlab.data.remote.ManagerMediaApi]/[ai.rojan.designlab.data.remote.SalonApi] call, scoped to this specialist's PORTFOLIO. */
    @GET("api/v1/salons/{salonId}/media")
    suspend fun getMedia(
        @Path("salonId") salonId: String,
        @Query("mediaType") mediaType: String,
        @Query("targetId") targetId: String,
    ): List<MediaAssetResponseDto>

    /**
     * Customer Specialist -> Services Integration: the real service ids this
     * specialist is eligible to perform. Raw `List<UUID>` on the backend
     * (`SpecialistController.listEligibleServices`) - **empty means eligible
     * for every service in the salon**, per that endpoint's own
     * `@Operation` summary, never "assigned to nothing".
     */
    @GET("api/v1/salons/{salonId}/specialists/{specialistId}/services")
    suspend fun getAssignedServiceIds(
        @Path("salonId") salonId: String,
        @Path("specialistId") specialistId: String,
    ): List<String>

    @POST("api/v1/salons/{salonId}/specialists")
    suspend fun createSpecialist(
        @Path("salonId") salonId: String,
        @Body request: CreateSpecialistRequestDto,
    ): SpecialistResponseDto

    @PUT("api/v1/salons/{salonId}/specialists/{specialistId}")
    suspend fun updateSpecialist(
        @Path("salonId") salonId: String,
        @Path("specialistId") specialistId: String,
        @Body request: UpdateSpecialistRequestDto,
    ): SpecialistResponseDto
}
