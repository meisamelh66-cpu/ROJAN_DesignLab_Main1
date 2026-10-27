package ai.rojan.designlab.manager.domain.repository

import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus

/**
 * Manager Domain Foundation Phase 1. [create] is suspend/[Result]-typed
 * (Phase 2, M1) — it's a real network call
 * (`ai.rojan.designlab.manager.data.BackendAppointmentRepository`'s own
 * doc comment). update/updateStatus stay synchronous and local-cache-only
 * - the backend has no owner-side generic update endpoint. See each
 * method's own doc comment below for the future real contract (Phase 11
 * Step 2's backend specification) that should replace it once that
 * endpoint exists. [cancel] is no longer in that group — see its own doc
 * comment (Final Blocker Closure Sprint).
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
     * Confirmed dead code (zero call sites anywhere in the Manager package,
     * verified by search) - kept rather than deleted since no real backend
     * counterpart exists yet to replace it with.
     */
    fun update(appointment: Appointment): Appointment?

    /** **Local-cache-only — not backend-persistent.** Same caveats as [update]. */
    fun updateStatus(id: String, status: AppointmentStatus): Appointment?

    /**
     * Real, backend-persistent (Final Blocker Closure Sprint): `PATCH
     * /api/v1/bookings/{id}/cancel`, moving a booking to `CANCELLED`.
     * Confirmed by direct read of `ai.rojan.backend.api.booking.BookingController.cancel`
     * - same top-level `/bookings/{id}/...` shape as [confirm]/[complete],
     * not nested under `/salons/{salonId}/...`. No longer local-cache-only
     * (was previously implemented via [updateStatus] with
     * [AppointmentStatus.CANCELLED], silently overwritten on the next
     * sync and never actually persisted) - see
     * [ai.rojan.designlab.manager.data.BackendAppointmentRepository]'s
     * own doc comment for the real implementation, which follows
     * [confirm]'s exact pattern.
     */
    suspend fun cancel(id: String): Result<Appointment>

    suspend fun createForCustomer(
        customerId: String,
        serviceId: String,
        specialistId: String,
        startTime: String,
        notes: String?,
    ): Result<Appointment>

    /**
     * Real, backend-persistent (RBAC compatibility fix — Manager Android
     * Pilot): `PATCH /api/v1/bookings/{id}/confirm`, moving a `PENDING`
     * booking to `CONFIRMED`. Distinct from [updateStatus], which stays
     * local-cache-only for the transitions that still have no real backend
     * endpoint wired up. See [ai.rojan.designlab.data.remote.ManagerBookingApi]'s
     * own doc comment for why this is real despite this interface's other
     * status-mutation methods not being.
     */
    suspend fun confirm(id: String): Result<Appointment>

    /** Real, backend-persistent — same shape as [confirm]: `PATCH /api/v1/bookings/{id}/complete`, moving a `CONFIRMED` booking to `COMPLETED`. */
    suspend fun complete(id: String): Result<Appointment>
}
