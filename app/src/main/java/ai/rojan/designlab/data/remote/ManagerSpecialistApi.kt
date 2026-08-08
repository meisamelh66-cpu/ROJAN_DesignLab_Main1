package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.CreateSpecialistRequestDto
import ai.rojan.designlab.data.remote.dto.SpecialistResponseDto
import ai.rojan.designlab.data.remote.dto.UpdateSpecialistRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Owner-only write operations (+ the eligible-services sub-resource) for
 * the ROJAN backend's Specialist API (`ROJAN_Backend/API_CONTRACT.md`).
 * Reads reuse the existing [SpecialistApi] (same endpoints Customer already
 * calls) — mirrors the exact split [ManagerServiceApi] already established
 * for Services.
 */
interface ManagerSpecialistApi {

    @POST("api/v1/salons/{salonId}/specialists")
    suspend fun create(
        @Path("salonId") salonId: String,
        @Body request: CreateSpecialistRequestDto,
    ): SpecialistResponseDto

    @PUT("api/v1/salons/{salonId}/specialists/{specialistId}")
    suspend fun update(
        @Path("salonId") salonId: String,
        @Path("specialistId") specialistId: String,
        @Body request: UpdateSpecialistRequestDto,
    ): SpecialistResponseDto

    @DELETE("api/v1/salons/{salonId}/specialists/{specialistId}")
    suspend fun deactivate(
        @Path("salonId") salonId: String,
        @Path("specialistId") specialistId: String,
    )

    /**
     * Eligible/assigned service ids for this specialist — empty means
     * eligible for every service in the salon (per the backend's own
     * contract). Real substitute for the old free-text `skills` field —
     * see [ai.rojan.designlab.manager.data.BackendSpecialistRepository].
     */
    @GET("api/v1/salons/{salonId}/specialists/{specialistId}/services")
    suspend fun eligibleServiceIds(
        @Path("salonId") salonId: String,
        @Path("specialistId") specialistId: String,
    ): List<String>
}
