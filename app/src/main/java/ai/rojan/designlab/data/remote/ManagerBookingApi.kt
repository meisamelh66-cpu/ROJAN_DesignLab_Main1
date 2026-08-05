package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.BookingResponseDto
import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Owner-only read of a salon's bookings (`ROJAN_Backend/API_CONTRACT.md`,
 * `SalonBookingController`). There is deliberately no create/update/cancel
 * method here — the backend has no owner-side booking-write endpoint
 * (only the customer-self-service `POST /api/v1/bookings`, which derives
 * `customerId` from the caller's own JWT, not a body field — using it from
 * the Manager app would silently attribute the booking to the manager,
 * not the intended customer). See [ai.rojan.designlab.manager.data.BackendAppointmentRepository]
 * for how writes are handled instead.
 */
interface ManagerBookingApi {

    @GET("api/v1/salons/{salonId}/bookings")
    suspend fun list(
        @Path("salonId") salonId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100,
        @Query("status") status: String? = null,
    ): PagedResponseDto<BookingResponseDto>
}
