package ai.rojan.designlab.reception.domain.repository

import ai.rojan.designlab.domain.repository.Booking
import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.domain.repository.PagedResult

/**
 * Salon-scoped booking operations Reception needs — listing a salon's
 * bookings and creating one on behalf of an existing (linked) customer.
 * Distinct from the top-level [ai.rojan.designlab.domain.repository.BookingRepository]
 * (customer self-service `createBooking`/`myBookings`, plus the
 * booking-id-scoped `cancelBooking`/`confirmBooking`/`checkInBooking`/
 * `completeBooking`/`rescheduleBooking`, all reused as-is by Reception —
 * no need to duplicate those here).
 *
 * Backed by real, already-existing backend endpoints (`SalonBookingController`
 * — `GET`/`POST /api/v1/salons/{salonId}/bookings`). Reception Authentication
 * Fix (Phase 4), source-verified: `Permission.MANAGE_BOOKINGS` is granted to
 * the salon owner, `SalonRole.MANAGER`, and `SalonRole.RECEPTIONIST` alike
 * (`SalonPermissionResolver`/`SalonRole.permissions()`) — calling these as an
 * actual `SalonRole.RECEPTIONIST` member succeeds; this was previously
 * (incorrectly) documented here as "owner-only today, will legitimately
 * fail authorization" — that was true of an earlier backend state, not the
 * current one. No mock, no fake data: every implementation of this
 * interface must call the real endpoint.
 */
interface ReceptionBookingRepository {

    suspend fun listBookings(
        salonId: String,
        page: Int = 0,
        size: Int = 20,
        status: BookingStatus? = null,
    ): Result<PagedResult<Booking>>

    /**
     * `customerId` is a CRM [ai.rojan.designlab.reception.domain.repository.ReceptionCustomer]
     * id, not a `User` id — the target customer must already be linked to
     * an account (backend returns `409 CUSTOMER_NOT_LINKED_TO_ACCOUNT`
     * otherwise, per `ManagerBookingApi.createForCustomer`'s own doc
     * comment, reused here since this is the same real endpoint).
     */
    suspend fun createBookingForCustomer(
        salonId: String,
        customerId: String,
        serviceId: String,
        specialistId: String,
        startTime: String,
        notes: String?,
    ): Result<Booking>
}
