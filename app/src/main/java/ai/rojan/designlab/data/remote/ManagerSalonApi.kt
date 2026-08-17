package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.AssignIdentityMediaRequestDto
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
 * `ROJAN_Backend` source — `SalonController.kt`/`SalonDtos.kt`). Both
 * reuse [SalonResponseDto] as the response shape, same as [mine] already
 * did. `logoUrl`/`latitude`/`longitude` are real backend fields today:
 * [update] writes `latitude`/`longitude` via [UpdateSalonRequestDto],
 * and the response deserializes whatever the backend actually has
 * stored for all three — `null` only when a salon genuinely has none
 * set yet, not because the fields are unsupported. `logoUrl` has no
 * write path here still, since no upload endpoint exists yet to produce
 * a real URL for it.
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

    /** Central Salon Management — Salon Media UI. Mirrors `SalonController.assignIdentityMedia` exactly — see [AssignIdentityMediaRequestDto]'s own doc comment. */
    @PUT("api/v1/salons/{salonId}/identity-media")
    suspend fun assignIdentityMedia(
        @Path("salonId") salonId: String,
        @Body request: AssignIdentityMediaRequestDto,
    ): SalonResponseDto
}
