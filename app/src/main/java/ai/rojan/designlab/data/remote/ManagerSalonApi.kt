package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.SalonResponseDto
import retrofit2.http.GET

/**
 * Retrofit contract for the ROJAN backend's owner-scoped Salon API
 * (`ROJAN_Backend/API_CONTRACT.md`, `GET /api/v1/salons/mine`). Separate
 * from the existing [SalonApi] (which only covers the public
 * browse/get-by-id endpoints Customer uses) rather than added to it,
 * since this endpoint is owner-authenticated and has no meaning for the
 * Customer flavor.
 */
interface ManagerSalonApi {

    @GET("api/v1/salons/mine")
    suspend fun mine(): List<SalonResponseDto>
}
