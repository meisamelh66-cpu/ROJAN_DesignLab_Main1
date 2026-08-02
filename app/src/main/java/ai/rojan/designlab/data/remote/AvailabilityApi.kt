package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.TimeSlotResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit contract for the ROJAN backend's computed-availability API (`ROJAN_Backend/API_CONTRACT.md`). */
interface AvailabilityApi {

    @GET("api/v1/salons/{salonId}/specialists/{specialistId}/available-slots")
    suspend fun getAvailableSlots(
        @Path("salonId") salonId: String,
        @Path("specialistId") specialistId: String,
        @Query("serviceId") serviceId: String,
        @Query("date") date: String,
        @Query("slotIntervalMinutes") slotIntervalMinutes: Int,
    ): List<TimeSlotResponseDto>
}
