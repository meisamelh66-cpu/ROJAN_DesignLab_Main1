package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.BookingResponseDto
import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * TEAM2-002 (Manager Data Persistence). Retrofit contract for the ROJAN
 * backend's salon-scoped booking listing (`ROJAN_Backend`'s
 * `SalonBookingController`) — distinct from [BookingApi]'s `mine`, which
 * lists the authenticated *customer's* own bookings. This is the salon
 * *owner's* view: every booking made against their salon, not just ones
 * they made themselves. Owner-only on the backend; a non-owner caller
 * gets 403.
 */
interface SalonBookingApi {

    @GET("api/v1/salons/{salonId}/bookings")
    suspend fun bookings(
        @Path("salonId") salonId: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("status") status: String?,
    ): PagedResponseDto<BookingResponseDto>
}
