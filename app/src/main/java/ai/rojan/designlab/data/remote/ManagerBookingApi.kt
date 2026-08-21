package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.BookingResponseDto
import ai.rojan.designlab.data.remote.dto.CreateBookingForCustomerRequestDto
import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Owner/manager read/create/status-mutation of a salon's bookings
 * (`ROJAN_Backend/API_CONTRACT.md`, `SalonBookingController`/
 * `BookingController`). [createForCustomer] is the owner-authorized
 * counterpart to the customer-self-service `POST /api/v1/bookings` (which
 * derives `customerId` from the caller's own JWT, and stays exactly as-is
 * for Customer) — it takes an explicit Customer CRM id instead, so it's
 * safe for the Manager app to call on a customer's behalf.
 *
 * [confirm]/[complete] (RBAC compatibility fix — Manager Android Pilot):
 * confirmed real and already RBAC-correct on the backend
 * (`ai.rojan.backend.api.booking.BookingController.confirm`/`complete`,
 * `PATCH /api/v1/bookings/{bookingId}/confirm`|`/complete` — note: the
 * top-level `/bookings/{id}/...` path, not nested under
 * `/salons/{salonId}/...` like [list]/[createForCustomer]; the backend's
 * `ConfirmBookingUseCase`/`CompleteBookingUseCase` each already gate on
 * `SalonPermissionResolver.require(booking.salonId, callerId,
 * Permission.MANAGE_BOOKINGS)`, which `SalonRole.MANAGER` already holds —
 * their OpenAPI summaries still say "salon owner only", which is stale
 * wording, not the real enforcement). This correction supersedes this
 * interface's own previous doc comment, which had confirmed these absent
 * as of an earlier audit — reconfirmed present by direct backend source
 * inspection this pass. `cancel`/`reschedule` are still not added here:
 * unlike confirm/complete, those were not part of this fix's approved
 * scope, so their absence (and [ai.rojan.designlab.manager.data.BackendAppointmentRepository]'s
 * pre-existing local-cache-only handling of them) is left exactly as-is.
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

    @PATCH("api/v1/bookings/{bookingId}/confirm")
    suspend fun confirm(@Path("bookingId") bookingId: String): BookingResponseDto

    @PATCH("api/v1/bookings/{bookingId}/complete")
    suspend fun complete(@Path("bookingId") bookingId: String): BookingResponseDto
}
