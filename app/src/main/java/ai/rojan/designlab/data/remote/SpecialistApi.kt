package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.SpecialistResponseDto
import retrofit2.http.GET
import retrofit2.http.Path

/** Retrofit contract for the ROJAN backend's Specialist API (`ROJAN_Backend/API_CONTRACT.md`). */
interface SpecialistApi {

    @GET("api/v1/salons/{salonId}/specialists")
    suspend fun getSpecialists(@Path("salonId") salonId: String): List<SpecialistResponseDto>

    @GET("api/v1/salons/{salonId}/specialists/{specialistId}")
    suspend fun getSpecialist(
        @Path("salonId") salonId: String,
        @Path("specialistId") specialistId: String,
    ): SpecialistResponseDto
}
