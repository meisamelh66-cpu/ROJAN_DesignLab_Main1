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
 * Owner-only read/create of a salon's bookings (`ROJAN_Backend/API_CONTRACT.md`,
 * `SalonBookingController`). [createForCustomer] is the owner-authorized
 * counterpart to the customer-self-service `POST /api/v1/bookings` (which
 * derives `customerId` from the caller's own JWT, and stays exactly as-is
 * for Customer) — it takes an explicit Customer CRM id instead, so it's
 * safe for the Manager app to call on a customer's behalf. There is still
 * no owner-side update/cancel-booking endpoint; see
 * [ai.rojan.designlab.manager.data.BackendAppointmentRepository] for how
 * those stay local-cache-only.
 *
 * **Future landing point (Phase 11 Step 2 backend specification, not yet
 * implemented backend-side — confirmed absent as of that audit, not
 * inferred from naming convention):** once real, this interface is where
 * the following would be added, matching [list]/[createForCustomer]'s
 * existing shape —
 * `GET api/v1/salons/{salonId}/bookings/{bookingId}` (detail),
 * `PATCH api/v1/salons/{salonId}/bookings/{bookingId}/cancel` (no body,
 * returns [BookingResponseDto]), and
 * `PUT api/v1/salons/{salonId}/bookings/{bookingId}/reschedule` (reusing
 * the existing `RescheduleBookingRequestDto` from `BookingDtos.kt` as-is —
 * no new DTO needed). None of these are added here now: the backend
 * contract doesn't exist yet, and adding the method signatures ahead of a
 * real endpoint would let the app compile against a call that 404s at
 * runtime.
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
