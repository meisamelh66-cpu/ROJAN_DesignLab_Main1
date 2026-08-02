package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.ServiceResponseDto
import retrofit2.http.GET
import retrofit2.http.Path

/** Retrofit contract for the ROJAN backend's Service API (`ROJAN_Backend/API_CONTRACT.md`). */
interface ServiceApi {

    @GET("api/v1/salons/{salonId}/categories/{categoryId}/services")
    suspend fun getServices(
        @Path("salonId") salonId: String,
        @Path("categoryId") categoryId: String,
    ): List<ServiceResponseDto>
}
