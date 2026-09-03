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

    /**
     * Real, backend-persistent (branch-integration reconciliation) — same
     * shape as [confirm]/[complete]: `PATCH /api/v1/bookings/{id}/cancel`,
     * moving a `PENDING`/`CONFIRMED` booking to `CANCELLED`. Confirmed
     * against backend source; see
     * [ai.rojan.designlab.data.remote.ManagerBookingApi]'s own doc comment
     * for this endpoint's (slightly broader) authorization rule. Replaces
     * the previous local-cache-only synchronous `cancel(id): Appointment?`
     * — [update]/[updateStatus] remain local-cache-only for the
     * transitions that still have no real backend endpoint.
     */
    suspend fun cancel(id: String): Result<Appointment>
}
