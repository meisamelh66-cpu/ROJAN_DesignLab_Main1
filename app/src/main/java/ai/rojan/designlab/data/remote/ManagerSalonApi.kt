package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.CreateSalonRequestDto
import ai.rojan.designlab.data.remote.dto.SalonResponseDto
import ai.rojan.designlab.data.remote.dto.UpdateSalonRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Retrofit contract for the ROJAN backend's owner-scoped Salon API
 * (`ROJAN_Backend/API_CONTRACT.md`, `GET /api/v1/salons/mine`). Separate
 * from the existing [SalonApi] (which only covers the public
 * browse/get-by-id endpoints Customer uses) rather than added to it,
 * since this endpoint is owner-authenticated and has no meaning for the
 * Customer flavor.
 *
 * Phase A — Owner Salon Identity: [create]/[update] added, mirroring
 * `SalonController.create`/`.update` exactly (verified directly against
 * backend source in `ROJAN_PhaseA_Salon_Identity_Readiness_Report_v1.md`
 * §1-2). Both reuse [SalonResponseDto] as the response shape, same as
 * [mine] already did — that DTO's `logoUrl`/`latitude`/`longitude` fields
 * still have no backend field behind them yet and simply deserialize to
 * `null` here too, same as everywhere else they're already used.
 */
interface ManagerSalonApi {

    @GET("api/v1/salons/mine")
    suspend fun mine(): List<SalonResponseDto>

    @POST("api/v1/salons")
    suspend fun create(@Body request: CreateSalonRequestDto): SalonResponseDto

    @PUT("api/v1/salons/{salonId}")
    suspend fun update(
        @Path("salonId") salonId: String,
        @Body request: UpdateSalonRequestDto,
    ): SalonResponseDto
}
