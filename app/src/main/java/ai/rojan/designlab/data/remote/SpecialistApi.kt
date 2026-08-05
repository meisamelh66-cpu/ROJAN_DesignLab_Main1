package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.CreateSpecialistRequestDto
import ai.rojan.designlab.data.remote.dto.SpecialistResponseDto
import ai.rojan.designlab.data.remote.dto.UpdateSpecialistRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Retrofit contract for the ROJAN backend's Specialist API (`ROJAN_Backend/API_CONTRACT.md`). `createSpecialist`/`updateSpecialist` are owner-only. */
interface SpecialistApi {

    @GET("api/v1/salons/{salonId}/specialists")
    suspend fun getSpecialists(@Path("salonId") salonId: String): List<SpecialistResponseDto>

    @GET("api/v1/salons/{salonId}/specialists/{specialistId}")
    suspend fun getSpecialist(
        @Path("salonId") salonId: String,
        @Path("specialistId") specialistId: String,
    ): SpecialistResponseDto

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
