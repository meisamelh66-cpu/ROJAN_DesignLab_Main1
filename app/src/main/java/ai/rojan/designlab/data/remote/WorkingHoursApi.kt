package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.WorkingHoursResponseDto
import retrofit2.http.GET
import retrofit2.http.Path

/** Retrofit contract for the ROJAN backend's Working Hours API (`GET /api/v1/salons/{salonId}/working-hours` - public read, no auth required, mirrors PublicSalonApi's unauthenticated-read shape). */
interface WorkingHoursApi {

    @GET("api/v1/salons/{salonId}/working-hours")
    suspend fun getWorkingHours(@Path("salonId") salonId: String): List<WorkingHoursResponseDto>
}
