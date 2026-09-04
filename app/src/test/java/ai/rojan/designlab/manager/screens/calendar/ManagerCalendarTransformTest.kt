package ai.rojan.designlab.manager.screens.calendar

import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus as DomainAppointmentStatus
import ai.rojan.designlab.manager.domain.customer.CustomerTag
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import ai.rojan.designlab.manager.domain.service.Service
import ai.rojan.designlab.manager.domain.specialist.Specialist
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Sprint 5B-4 (5B4-1 / 5B4-2): covers the pure calendar-day transformation
 * extracted from [ManagerCalendarScreen] — grouping slice + specialist
 * filter + chronological sort + id→name resolution via the snapshot maps.
 */
class ManagerCalendarTransformTest {

    private fun appointment(
        id: String,
        date: String,
        time: String,
        customerId: String = "c1",
        serviceId: String = "s1",
        specialistId: String = "sp1",
        status: DomainAppointmentStatus = DomainAppointmentStatus.CONFIRMED,
    ) = Appointment(
        id = id,
        customerId = customerId,
        serviceId = serviceId,
        specialistId = specialistId,
        date = date,
        time = time,
        status = status,
    )

    private fun customer(id: String, name: String) =
        ManagerCustomer(id, name, "0", CustomerTag.REGULAR, 0, null, "2026/09/01", 0)

    private fun service(id: String, name: String) =
        Service(id, name, "cat", 0L, 30, true)

    private fun specialist(id: String, name: String) =
        Specialist(id, name, emptyList(), "", 0.0, true)

    private val customersById = listOf(customer("c1", "سارا"), customer("c2", "مینا")).associateBy { it.id }
    private val servicesById = listOf(service("s1", "کوتاهی"), service("s2", "رنگ")).associateBy { it.id }
    private val specialistsById = listOf(specialist("sp1", "الهام"), specialist("sp2", "نازنین")).associateBy { it.id }

    private fun run(
        appointmentsForDay: List<Appointment>,
        selectedSpecialistId: String? = null,
    ) = calendarDayAppointments(
        appointmentsForDay = appointmentsForDay,
        selectedSpecialistId = selectedSpecialistId,
        customersById = customersById,
        servicesById = servicesById,
        specialistsById = specialistsById,
    )

    @Test
    fun `groupBy slice feeds only that day's appointments into the transform`() {
        val all = listOf(
            appointment("a1", date = "2026/09/01", time = "10:00"),
            appointment("a2", date = "2026/09/02", time = "09:00"),
            appointment("a3", date = "2026/09/01", time = "12:00"),
        )
        val byDay = all.groupBy { it.date }

        assertEquals(listOf("a1", "a3"), run(byDay["2026/09/01"].orEmpty()).map { it.id })
        assertEquals(listOf("a2"), run(byDay["2026/09/02"].orEmpty()).map { it.id })
    }

    @Test
    fun `null selected specialist returns every appointment`() {
        val day = listOf(
            appointment("a1", "2026/09/01", "10:00", specialistId = "sp1"),
            appointment("a2", "2026/09/01", "11:00", specialistId = "sp2"),
        )
        assertEquals(listOf("a1", "a2"), run(day, selectedSpecialistId = null).map { it.id })
    }

    @Test
    fun `a selected specialist keeps only that specialist's appointments`() {
        val day = listOf(
            appointment("a1", "2026/09/01", "10:00", specialistId = "sp1"),
            appointment("a2", "2026/09/01", "11:00", specialistId = "sp2"),
            appointment("a3", "2026/09/01", "12:00", specialistId = "sp1"),
        )
        assertEquals(listOf("a1", "a3"), run(day, selectedSpecialistId = "sp1").map { it.id })
    }

    @Test
    fun `appointments are sorted chronologically by time`() {
        val day = listOf(
            appointment("late", "2026/09/01", "16:30"),
            appointment("early", "2026/09/01", "08:15"),
            appointment("mid", "2026/09/01", "12:00"),
        )
        assertEquals(listOf("early", "mid", "late"), run(day).map { it.id })
    }

    @Test
    fun `an empty day produces an empty result`() {
        assertEquals(emptyList<String>(), run(emptyList()).map { it.id })
    }

    @Test
    fun `display mapping resolves customer, service and specialist names`() {
        val row = run(
            listOf(appointment("a1", "2026/09/01", "10:00", customerId = "c2", serviceId = "s2", specialistId = "sp2")),
        ).single()

        assertEquals("مینا", row.clientName)
        assertEquals("رنگ", row.service)
        assertEquals("نازنین", row.specialist)
        assertEquals("10:00", row.time)
    }

    @Test
    fun `unknown ids fall back to em dash exactly as before`() {
        val row = run(
            listOf(appointment("a1", "2026/09/01", "10:00", customerId = "ghost", serviceId = "ghost", specialistId = "ghost")),
        ).single()

        assertEquals("—", row.clientName)
        assertEquals("—", row.service)
        assertEquals("—", row.specialist)
    }

    @Test
    fun `domain status maps to the matching display status`() {
        fun statusOf(domain: DomainAppointmentStatus) =
            run(listOf(appointment("a1", "2026/09/01", "10:00", status = domain))).single().status

        assertEquals(AppointmentStatus.PENDING, statusOf(DomainAppointmentStatus.PENDING))
        assertEquals(AppointmentStatus.CONFIRMED, statusOf(DomainAppointmentStatus.CONFIRMED))
        assertEquals(AppointmentStatus.COMPLETED, statusOf(DomainAppointmentStatus.COMPLETED))
        assertEquals(AppointmentStatus.CANCELLED, statusOf(DomainAppointmentStatus.CANCELLED))
        assertEquals(AppointmentStatus.NO_SHOW, statusOf(DomainAppointmentStatus.NO_SHOW))
    }
}
