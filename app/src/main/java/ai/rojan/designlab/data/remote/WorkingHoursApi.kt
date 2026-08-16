package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.SetWorkingHoursRequestDto
import ai.rojan.designlab.data.remote.dto.WorkingHoursResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Retrofit contract for the ROJAN backend's Working Hours API
 * (`ai.rojan.backend.api.schedule.WorkingHoursController`). `GET` is public,
 * no auth required (mirrors `PublicSalonApi`'s unauthenticated-read shape);
 * `PUT`/`DELETE` are owner-only, enforced server-side — same convention as
 * every other salon-scoped write endpoint (`ManagerSalonApi`).
 */
interface WorkingHoursApi {

    @GET("api/v1/salons/{salonId}/working-hours")
    suspend fun getWorkingHours(@Path("salonId") salonId: String): List<WorkingHoursResponseDto>

    @PUT("api/v1/salons/{salonId}/working-hours/{dayOfWeek}")
    suspend fun setWorkingHours(
        @Path("salonId") salonId: String,
        @Path("dayOfWeek") dayOfWeek: String,
        @Body request: SetWorkingHoursRequestDto,
    ): WorkingHoursResponseDto

    @DELETE("api/v1/salons/{salonId}/working-hours/{dayOfWeek}")
    suspend fun removeWorkingHours(
        @Path("salonId") salonId: String,
        @Path("dayOfWeek") dayOfWeek: String,
    )
}
