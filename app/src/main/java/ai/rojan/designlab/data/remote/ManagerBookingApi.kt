package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.BookingResponseDto
import ai.rojan.designlab.data.remote.dto.CreateBookingForCustomerRequestDto
import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Owner-scoped bookings (`ROJAN_Backend/api/booking/SalonBookingController.kt`).
 *
 * `createForCustomer` is now real (added on `feature/auth-rate-limit-finalization`, verified by
 * direct read, not assumed) — the owner-authorized counterpart to the customer-self-service
 * `POST /api/v1/bookings`, taking an explicit `customerId` rather than deriving it from the
 * caller's JWT. **This method is implemented and correct, but is deliberately not wired into the
 * Manager Booking wizard's "confirm" action yet** — see
 * [ai.rojan.designlab.manager.data.BackendAppointmentRepository]'s doc comment for exactly why
 * (the wizard's date/time selection is still built on a pre-existing fake static calendar week,
 * with no reliable conversion to the real `LocalDateTime` this endpoint needs; sending a wrong
 * `startTime` would create a real, wrong appointment, which is worse than not integrating yet).
 */
interface ManagerBookingApi {

    @GET("api/v1/salons/{salonId}/bookings")
    suspend fun list(
        @Path("salonId") salonId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100,
        @Query("status") status: String? = null,
    ): PagedResponseDto<BookingResponseDto>

    @POST("api/v1/salons/{salonId}/bookings")
    suspend fun createForCustomer(
        @Path("salonId") salonId: String,
        @Body request: CreateBookingForCustomerRequestDto,
    ): BookingResponseDto
}
