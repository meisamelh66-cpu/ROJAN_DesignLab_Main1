package ai.rojan.designlab.manager.screens.calendar

import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus as DomainAppointmentStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Issue-012 regression coverage (second instance): [Appointment.customerId] is a `UserId`, never
 * a CRM `Customer.id` - [Appointment.toDisplay] must resolve the client name through the real
 * `getByUserId` lookup, not `getById`, the exact same conflation already fixed on
 * `ManagerAppointmentDetailScreen` but previously missed here.
 *
 * Uses the [Appointment.toDisplay]'s `resolveCustomerName` lambda seam - never touches the
 * `ManagerRepositories` singleton, so no fake [ai.rojan.designlab.manager.domain.repository.CustomerRepository]
 * or test teardown is needed.
 */
class ManagerCalendarScreenKtTest {

    private fun appointment(customerId: String) = Appointment(
        id = "appt-1",
        customerId = customerId,
        serviceId = "service-1",
        specialistId = "specialist-1",
        date = "2026-10-16",
        time = "10:00",
        status = DomainAppointmentStatus.CONFIRMED,
    )

    @Test
    fun `an appointment linked by userId displays the real customer name, not the dash fallback`() {
        val display = appointment(customerId = "user-1").toDisplay(
            resolveCustomerName = { id -> if (id == "user-1") "Sara" else null },
        )

        assertEquals("Sara", display.clientName)
    }

    @Test
    fun `an unresolved customer name falls back to the dash, not a crash or a fabricated name`() {
        val display = appointment(customerId = "user-with-no-crm-record").toDisplay(
            resolveCustomerName = { null },
        )

        assertEquals("—", display.clientName)
    }
}
