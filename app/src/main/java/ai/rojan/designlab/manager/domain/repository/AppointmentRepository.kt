package ai.rojan.designlab.manager.domain.repository

import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus

/**
 * Manager Domain Foundation Phase 1. Create/update/cancel are part of
 * the contract now (not wired to any screen yet) so P0's booking
 * journey doesn't need a second breaking change to this interface later.
 */
interface AppointmentRepository {
    fun getAll(): List<Appointment>
    fun getById(id: String): Appointment?
    fun getByCustomerId(customerId: String): List<Appointment>
    fun create(appointment: Appointment): Appointment
    fun update(appointment: Appointment): Appointment?
    fun updateStatus(id: String, status: AppointmentStatus): Appointment?
    fun cancel(id: String): Appointment?
}
