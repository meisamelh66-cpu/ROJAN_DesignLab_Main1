package ai.rojan.designlab.manager.domain.ai

import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus
import ai.rojan.designlab.manager.domain.customer.CustomerTag
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Manager CRM AI Foundation, Phase 8 Step 2 — hermetic coverage of
 * [VipWithoutAppointmentsInsightProvider], the first rule exercising
 * [ManagerCrmInsightContext.appointments].
 */
class VipWithoutAppointmentsInsightProviderTest {

    private val provider = VipWithoutAppointmentsInsightProvider()

    private fun customer(id: String, tag: CustomerTag, name: String = "Customer $id") = ManagerCustomer(
        id = id,
        name = name,
        phone = "+98912000$id",
        tag = tag,
        loyaltyScore = 0,
        notes = null,
        lastVisit = "—",
        totalVisits = 0,
    )

    private fun appointment(id: String, customerId: String) = Appointment(
        id = id,
        customerId = customerId,
        serviceId = "svc-1",
        specialistId = "spec-1",
        date = "1404/01/01",
        time = "10:00",
        status = AppointmentStatus.CONFIRMED,
    )

    @Test
    fun `zero customers yields no insights`() {
        val insights = provider.insightsFor(
            ManagerCrmInsightContext(salonId = "s1", customers = emptyList(), appointments = emptyList()),
        )

        assertTrue(insights.isEmpty())
    }

    @Test
    fun `a VIP customer with no appointments produces an insight`() {
        val customers = listOf(customer("c1", CustomerTag.VIP, name = "Sara"))

        val insights = provider.insightsFor(
            ManagerCrmInsightContext(salonId = "s1", customers = customers, appointments = emptyList()),
        )

        assertEquals(1, insights.size)
        val insight = insights.single()
        assertEquals("c1", insight.customerId)
        assertEquals("vip-without-appointments-c1", insight.id)
        assertEquals(ManagerCrmInsightCategory.VIP_WITHOUT_APPOINTMENTS, insight.category)
        assertEquals("Sara", insight.title)
    }

    @Test
    fun `a VIP customer with a synced appointment produces no insight`() {
        val customers = listOf(customer("c1", CustomerTag.VIP))
        val appointments = listOf(appointment("a1", customerId = "c1"))

        val insights = provider.insightsFor(
            ManagerCrmInsightContext(salonId = "s1", customers = customers, appointments = appointments),
        )

        assertTrue(insights.isEmpty())
    }

    @Test
    fun `a non-VIP customer with no appointments produces no insight`() {
        val customers = listOf(
            customer("c1", CustomerTag.INACTIVE),
            customer("c2", CustomerTag.NEW),
            customer("c3", CustomerTag.REGULAR),
        )

        val insights = provider.insightsFor(
            ManagerCrmInsightContext(salonId = "s1", customers = customers, appointments = emptyList()),
        )

        assertTrue(insights.isEmpty())
    }

    @Test
    fun `only the VIP customer without appointments is surfaced from a mixed set`() {
        val customers = listOf(
            customer("c1", CustomerTag.VIP), // no appointment -> insight
            customer("c2", CustomerTag.VIP), // has appointment -> no insight
            customer("c3", CustomerTag.INACTIVE), // not VIP -> no insight
            customer("c4", CustomerTag.VIP), // no appointment -> insight
        )
        val appointments = listOf(appointment("a1", customerId = "c2"))

        val insights = provider.insightsFor(
            ManagerCrmInsightContext(salonId = "s1", customers = customers, appointments = appointments),
        )

        assertEquals(2, insights.size)
        assertEquals(setOf("c1", "c4"), insights.map { it.customerId }.toSet())
    }

    @Test
    fun `each insight id and customerId are stable across calls`() {
        val customers = listOf(customer("c-9", CustomerTag.VIP, name = "Reza"))
        val context = ManagerCrmInsightContext(salonId = "s1", customers = customers, appointments = emptyList())

        val first = provider.insightsFor(context)
        val second = provider.insightsFor(context)

        assertEquals("vip-without-appointments-c-9", first.single().id)
        assertEquals(first.single().id, second.single().id)
        assertEquals("c-9", first.single().customerId)
        assertEquals(first.single().customerId, second.single().customerId)
    }

    @Test
    fun `the reason states only that no synced appointment exists, never a time or behavior claim`() {
        val customers = listOf(customer("c1", CustomerTag.VIP))

        val reason = provider.insightsFor(
            ManagerCrmInsightContext(salonId = "s1", customers = customers, appointments = emptyList()),
        ).single().reason

        assertTrue(reason.none { it.isDigit() })
    }
}
