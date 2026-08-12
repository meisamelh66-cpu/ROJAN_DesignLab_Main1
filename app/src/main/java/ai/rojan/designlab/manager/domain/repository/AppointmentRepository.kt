package ai.rojan.designlab.manager.domain.repository

import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus

/**
 * Manager Domain Foundation Phase 1. [create] is suspend/[Result]-typed
 * (Phase 2, M1) — it's a real network call
 * (`ai.rojan.designlab.manager.data.BackendAppointmentRepository`'s own
 * doc comment). update/updateStatus/cancel stay synchronous and
 * local-cache-only - the backend has no owner-side update/cancel-booking
 * endpoint yet. See each method's own doc comment below for the future
 * real contract (Phase 11 Step 2's backend specification) that should
 * replace it once that endpoint exists.
 *
 * [createForCustomer] (Final Release Validation — Real Booking Calendar
 * Integration): the real, owner-authorized booking write, backed by
 * `POST /api/v1/salons/{salonId}/bookings`. [startTime] must be a value
 * that came directly from the computed-availability API's response, not
 * a reconstructed one — see [ai.rojan.designlab.manager.domain.booking.ManagerBookingState.time]'s
 * doc comment for why. Distinct from [create], which stays local-cache-only.
 */
interface AppointmentRepository {
    fun getAll(): List<Appointment>
    fun getById(id: String): Appointment?
    fun getByCustomerId(customerId: String): List<Appointment>
    suspend fun create(appointment: Appointment): Result<Appointment>

    /**
     * **Local-cache-only — not backend-persistent.** Mutates the in-memory
     * cache [getAll]/[getById] read from and returns immediately; makes no
     * network call, and is silently overwritten by the next
     * [ai.rojan.designlab.manager.data.BackendAppointmentRepository.sync].
     * Confirmed dead code as of Phase 11 Step 3 (zero call sites anywhere
     * in the Manager package, verified by search) - kept rather than
     * deleted since no real backend counterpart exists yet to replace it
     * with. See [cancel]'s doc comment for the planned real contract this
     * (and [updateStatus]) are standing in for.
     */
    fun update(appointment: Appointment): Appointment?

    /**
     * **Local-cache-only — not backend-persistent.** Same caveats as
     * [update]. [cancel] delegates to this with [AppointmentStatus.CANCELLED];
     * a future real status-mutation endpoint (if one is ever specified
     * beyond cancel) would replace this the same way [cancel]'s doc
     * comment describes.
     */
    fun updateStatus(id: String, status: AppointmentStatus): Appointment?

    /**
     * **Local-cache-only — not backend-persistent.** Same caveats as
     * [update]; delegates to [updateStatus] with [AppointmentStatus.CANCELLED].
     *
     * Future real contract (Phase 11 Step 2 backend specification — not
     * yet implemented backend-side, confirmed absent as of that audit):
     * `PATCH /api/v1/salons/{salonId}/bookings/{bookingId}/cancel`, no
     * request body, response `BookingResponseDto`, errors
     * `BOOKING_NOT_FOUND`/`UNAUTHORIZED_SALON_ACCESS`/`INVALID_STATUS_TRANSITION`.
     * When that endpoint is real, this should become `suspend fun
     * cancel(id: String): Result<Appointment>` (matching [create]/
     * [createForCustomer]'s shape), calling it via `safeApiCall` from
     * [ai.rojan.designlab.manager.data.BackendAppointmentRepository] -
     * not simply have this synchronous method's body swapped in place.
     */
    fun cancel(id: String): Appointment?

    suspend fun createForCustomer(
        customerId: String,
        serviceId: String,
        specialistId: String,
        startTime: String,
        notes: String?,
    ): Result<Appointment>
}
