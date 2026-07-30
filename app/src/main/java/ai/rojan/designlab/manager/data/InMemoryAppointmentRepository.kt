package ai.rojan.designlab.manager.data

import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus
import ai.rojan.designlab.manager.domain.repository.AppointmentRepository

/**
 * In-memory [AppointmentRepository] — new sample data, deliberately
 * independent from
 * [ai.rojan.designlab.manager.screens.calendar.ManagerCalendarScreen]'s
 * own screen-local sample set (left untouched this phase). Migrating
 * Calendar to consume this repository requires the screen itself to
 * resolve customerId/serviceId/specialistId to display names, which is
 * screen-rework out of Phase 1's scope — see the Phase 1 report.
 * customerId/serviceId/specialistId here reference real ids from
 * [InMemoryCustomerRepository]/[InMemoryServiceRepository]/
 * [InMemorySpecialistRepository] so a future join is consistent.
 */
class InMemoryAppointmentRepository : AppointmentRepository {

    private val appointments = mutableListOf(
        Appointment("apt1", customerId = "c1", serviceId = "s1", specialistId = "sp1", date = "۱۴۰۴/۰۵/۰۴", time = "۱۰:۰۰", status = AppointmentStatus.CONFIRMED),
        Appointment("apt2", customerId = "c2", serviceId = "s3", specialistId = "sp2", date = "۱۴۰۴/۰۵/۰۴", time = "۱۱:۳۰", status = AppointmentStatus.CONFIRMED),
        Appointment("apt3", customerId = "c3", serviceId = "s4", specialistId = "sp3", date = "۱۴۰۴/۰۵/۰۴", time = "۱۲:۳۰", status = AppointmentStatus.PENDING),
        Appointment("apt4", customerId = "c4", serviceId = "s2", specialistId = "sp1", date = "۱۴۰۴/۰۵/۰۴", time = "۱۴:۰۰", status = AppointmentStatus.COMPLETED),
        Appointment("apt5", customerId = "c5", serviceId = "s6", specialistId = "sp2", date = "۱۴۰۴/۰۵/۰۴", time = "۱۶:۳۰", status = AppointmentStatus.CANCELLED),
        Appointment("apt6", customerId = "c6", serviceId = "s7", specialistId = "sp3", date = "۱۴۰۴/۰۵/۰۵", time = "۰۹:۳۰", status = AppointmentStatus.CONFIRMED),
        Appointment("apt7", customerId = "c7", serviceId = "s1", specialistId = "sp1", date = "۱۴۰۴/۰۵/۰۵", time = "۱۳:۰۰", status = AppointmentStatus.NO_SHOW),
    )

    override fun getAll(): List<Appointment> = appointments.toList()

    override fun getById(id: String): Appointment? = appointments.find { it.id == id }

    override fun getByCustomerId(customerId: String): List<Appointment> =
        appointments.filter { it.customerId == customerId }

    override fun create(appointment: Appointment): Appointment {
        appointments.add(appointment)
        return appointment
    }

    override fun update(appointment: Appointment): Appointment? {
        val index = appointments.indexOfFirst { it.id == appointment.id }
        if (index == -1) return null
        appointments[index] = appointment
        return appointment
    }

    override fun updateStatus(id: String, status: AppointmentStatus): Appointment? {
        val index = appointments.indexOfFirst { it.id == id }
        if (index == -1) return null
        val updated = appointments[index].copy(status = status)
        appointments[index] = updated
        return updated
    }

    override fun cancel(id: String): Appointment? = updateStatus(id, AppointmentStatus.CANCELLED)
}
