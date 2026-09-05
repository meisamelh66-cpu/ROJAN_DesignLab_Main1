package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.UserResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Manager Booking Creation Integrity follow-up. Retrofit contract for the
 * ROJAN backend's salon-scoped customer search
 * (`ROJAN_Backend`'s `SalonCustomerController`) — owner-only; a
 * non-owner caller gets 403.
 */
interface SalonCustomerApi {

    @GET("api/v1/salons/{salonId}/customers")
    suspend fun search(
        @Path("salonId") salonId: String,
        @Query("query") query: String?,
    ): List<UserResponseDto>
}
