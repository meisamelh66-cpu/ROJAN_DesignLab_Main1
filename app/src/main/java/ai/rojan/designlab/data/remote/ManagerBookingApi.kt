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
 * `createForCustomer` is the owner-authorized counterpart to the customer-self-service
 * `POST /api/v1/bookings`: it takes an explicit `customerId` rather than deriving one from the
 * caller's JWT, which is what makes it usable from the Manager app at all. The endpoint itself is
 * implemented and correct here, but is deliberately not called from the Manager Booking wizard's
 * "confirm" action — see [ai.rojan.designlab.manager.data.BackendAppointmentRepository]'s doc
 * comment for why (the wizard's date/time selection is still a static placeholder calendar with no
 * reliable conversion to the real `LocalDateTime` this endpoint needs; a wrong `startTime` would
 * create a real, wrong appointment).
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
