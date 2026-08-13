package ai.rojan.designlab.reception.domain.repository

import ai.rojan.designlab.domain.repository.Booking
import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.domain.repository.PagedResult

/**
 * Salon-scoped booking operations Reception needs — listing a salon's
 * bookings and creating one on behalf of an existing (linked) customer.
 * Distinct from the top-level [ai.rojan.designlab.domain.repository.BookingRepository]
 * (customer self-service `createBooking`/`myBookings`, plus the
 * booking-id-scoped `cancelBooking`/`confirmBooking`/`completeBooking`/
 * `rescheduleBooking`, all reused as-is by Reception — no need to
 * duplicate those here).
 *
 * Backed by real, already-existing backend endpoints
 * (`SalonBookingController` — `GET`/`POST /api/v1/salons/{salonId}/bookings`,
 * verified against `origin/feature/auth-rate-limit-finalization`) that are
 * owner-only today. Calling this as a receptionist will legitimately fail
 * authorization until `ROJAN_System1_Backend_Decision_v2.md` §4 item 6
 * (authorization broadening) ships — that is the correct, honest outcome,
 * not a bug to work around client-side. No mock, no fake data: every
 * implementation of this interface must call the real endpoint.
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
