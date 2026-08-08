package ai.rojan.designlab.manager.domain.repository

import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus

/**
 * Manager Domain Foundation Phase 1. [create] is suspend/[Result]-typed
 * (Phase 2, M1) — it's a real network call
 * (`ai.rojan.designlab.manager.data.BackendAppointmentRepository`'s own
 * doc comment). update/updateStatus/cancel stay synchronous and
 * local-cache-only - the backend has no owner-side update/cancel-booking
 * endpoint yet.
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
    fun update(appointment: Appointment): Appointment?
    fun updateStatus(id: String, status: AppointmentStatus): Appointment?
    fun cancel(id: String): Appointment?
    suspend fun createForCustomer(
        customerId: String,
        serviceId: String,
        specialistId: String,
        startTime: String,
        notes: String?,
    ): Result<Appointment>
}
