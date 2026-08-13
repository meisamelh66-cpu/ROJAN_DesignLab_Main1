package ai.rojan.designlab.manager.domain.ai

import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus
import ai.rojan.designlab.manager.domain.customer.CustomerTag
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Manager CRM AI Foundation, Phase 7 Step 5 — hermetic coverage of
 * [CompositeManagerCrmInsightProvider] using small fake sub-providers, so
 * this test verifies only the combining behavior (order, concatenation),
 * never a real rule's own logic (already covered by
 * [InactiveCustomerInsightProviderTest]/[VipCustomerInsightProviderTest]/
 * [VipWithoutAppointmentsInsightProviderTest]).
 */
class CompositeManagerCrmInsightProviderTest {

    private fun insight(id: String, category: ManagerCrmInsightCategory) =
        ManagerCrmInsight(id = id, category = category, customerId = "customer-$id", title = "t-$id", reason = "r-$id")

    private class FakeProvider(private val result: List<ManagerCrmInsight>) : ManagerCrmInsightProvider {
        override fun insightsFor(context: ManagerCrmInsightContext): List<ManagerCrmInsight> = result
    }

    private val context = ManagerCrmInsightContext(salonId = "s1", customers = emptyList())

    @Test
    fun `zero providers yields no insights`() {
        val composite = CompositeManagerCrmInsightProvider(emptyList())

        assertTrue(composite.insightsFor(context).isEmpty())
    }

    @Test
    fun `a single provider's results pass through unchanged`() {
        val results = listOf(insight("a", ManagerCrmInsightCategory.INACTIVE_CUSTOMER))
        val composite = CompositeManagerCrmInsightProvider(listOf(FakeProvider(results)))

        assertEquals(results, composite.insightsFor(context))
    }

    @Test
    fun `two providers' results are concatenated in order, both categories preserved`() {
        val inactive = listOf(insight("i1", ManagerCrmInsightCategory.INACTIVE_CUSTOMER), insight("i2", ManagerCrmInsightCategory.INACTIVE_CUSTOMER))
        val vip = listOf(insight("v1", ManagerCrmInsightCategory.VIP_CUSTOMER))
        val composite = CompositeManagerCrmInsightProvider(listOf(FakeProvider(inactive), FakeProvider(vip)))

        val combined = composite.insightsFor(context)

        assertEquals(listOf("i1", "i2", "v1"), combined.map { it.id })
        assertEquals(2, combined.count { it.category == ManagerCrmInsightCategory.INACTIVE_CUSTOMER })
        assertEquals(1, combined.count { it.category == ManagerCrmInsightCategory.VIP_CUSTOMER })
    }

    @Test
    fun `an empty-result provider contributes nothing but doesn't break the others`() {
        val vip = listOf(insight("v1", ManagerCrmInsightCategory.VIP_CUSTOMER))
        val composite = CompositeManagerCrmInsightProvider(listOf(FakeProvider(emptyList()), FakeProvider(vip)))

        assertEquals(vip, composite.insightsFor(context))
    }

    @Test
    fun `the real inactive and VIP providers combine correctly over real customer data`() {
        val customers = listOf(
            ManagerCustomer(id = "c1", name = "A", phone = "1", tag = CustomerTag.INACTIVE, loyaltyScore = 0, notes = null, lastVisit = "—", totalVisits = 0),
            ManagerCustomer(id = "c2", name = "B", phone = "2", tag = CustomerTag.VIP, loyaltyScore = 0, notes = null, lastVisit = "—", totalVisits = 0),
            ManagerCustomer(id = "c3", name = "C", phone = "3", tag = CustomerTag.REGULAR, loyaltyScore = 0, notes = null, lastVisit = "—", totalVisits = 0),
        )
        val composite = CompositeManagerCrmInsightProvider(listOf(InactiveCustomerInsightProvider(), VipCustomerInsightProvider()))

        val combined = composite.insightsFor(ManagerCrmInsightContext(salonId = "s1", customers = customers))

        assertEquals(2, combined.size)
        assertEquals("inactive-customer-c1", combined[0].id)
        assertEquals("c1", combined[0].customerId)
        assertEquals("vip-customer-c2", combined[1].id)
        assertEquals("c2", combined[1].customerId)
    }

    @Test
    fun `Phase 8 Step 3 - all three real providers, including VipWithoutAppointments, combine correctly`() {
        val customers = listOf(
            ManagerCustomer(id = "c1", name = "A", phone = "1", tag = CustomerTag.INACTIVE, loyaltyScore = 0, notes = null, lastVisit = "—", totalVisits = 0),
            ManagerCustomer(id = "c2", name = "B", phone = "2", tag = CustomerTag.VIP, loyaltyScore = 0, notes = null, lastVisit = "—", totalVisits = 0),
            ManagerCustomer(id = "c3", name = "C", phone = "3", tag = CustomerTag.REGULAR, loyaltyScore = 0, notes = null, lastVisit = "—", totalVisits = 0),
            ManagerCustomer(id = "c4", name = "D", phone = "4", tag = CustomerTag.VIP, loyaltyScore = 0, notes = null, lastVisit = "—", totalVisits = 0),
        )
        val appointments = listOf(
            Appointment(id = "a1", customerId = "c2", serviceId = "svc", specialistId = "spec", date = "1404/01/01", time = "10:00", status = AppointmentStatus.CONFIRMED),
        )
        val composite = CompositeManagerCrmInsightProvider(
            listOf(InactiveCustomerInsightProvider(), VipCustomerInsightProvider(), VipWithoutAppointmentsInsightProvider()),
        )

        val combined = composite.insightsFor(ManagerCrmInsightContext(salonId = "s1", customers = customers, appointments = appointments))

        assertEquals(4, combined.size)
        assertEquals(listOf("inactive-customer-c1", "vip-customer-c2", "vip-customer-c4", "vip-without-appointments-c4"), combined.map { it.id })
        assertTrue(combined.any { it.category == ManagerCrmInsightCategory.VIP_WITHOUT_APPOINTMENTS && it.customerId == "c4" })
    }

    // --- CRM Insight Engine Hardening, Phase 8 Step 5 --------------------
    // Explicit, dedicated coverage of properties the tests above already
    // demonstrated incidentally but never asserted as their own guarantee:
    // a customer matching more than one rule keeps every insight, no
    // cross-provider de-duplication happens (even adversarially, by id),
    // and combining order stays stable. No production code changed for
    // this hardening pass - insightsFor()'s plain `flatMap` already
    // guarantees all of this by construction; these tests make that
    // guarantee explicit and regression-proof rather than incidental.

    @Test
    fun `a customer matching multiple real providers keeps every insight, none dropped`() {
        val vipWithoutAppointments = ManagerCustomer(
            id = "c1", name = "Sara", phone = "1", tag = CustomerTag.VIP,
            loyaltyScore = 0, notes = null, lastVisit = "—", totalVisits = 0,
        )
        val composite = CompositeManagerCrmInsightProvider(
            listOf(VipCustomerInsightProvider(), VipWithoutAppointmentsInsightProvider()),
        )

        val combined = composite.insightsFor(
            ManagerCrmInsightContext(salonId = "s1", customers = listOf(vipWithoutAppointments), appointments = emptyList()),
        )

        // Same customerId, two distinct categories, both present - not collapsed into one.
        assertEquals(2, combined.size)
        assertTrue(combined.all { it.customerId == "c1" })
        assertEquals(
            setOf(ManagerCrmInsightCategory.VIP_CUSTOMER, ManagerCrmInsightCategory.VIP_WITHOUT_APPOINTMENTS),
            combined.map { it.category }.toSet(),
        )
    }

    @Test
    fun `two providers emitting the identical insight id are both preserved, not overwritten`() {
        // Adversarial by construction: real providers never collide (distinct
        // id prefixes per rule), but nothing in the composite enforces that -
        // this proves the guarantee holds even if a future provider did collide.
        val fromFirst = insight("dup-id", ManagerCrmInsightCategory.INACTIVE_CUSTOMER)
        val fromSecond = insight("dup-id", ManagerCrmInsightCategory.VIP_CUSTOMER)
        val composite = CompositeManagerCrmInsightProvider(listOf(FakeProvider(listOf(fromFirst)), FakeProvider(listOf(fromSecond))))

        val combined = composite.insightsFor(context)

        assertEquals(2, combined.size)
        assertEquals(listOf(fromFirst, fromSecond), combined)
    }

    @Test
    fun `combining order is stable across repeated calls with the same providers and context`() {
        val customers = listOf(
            ManagerCustomer(id = "c1", name = "A", phone = "1", tag = CustomerTag.VIP, loyaltyScore = 0, notes = null, lastVisit = "—", totalVisits = 0),
            ManagerCustomer(id = "c2", name = "B", phone = "2", tag = CustomerTag.VIP, loyaltyScore = 0, notes = null, lastVisit = "—", totalVisits = 0),
        )
        val composite = CompositeManagerCrmInsightProvider(
            listOf(VipCustomerInsightProvider(), VipWithoutAppointmentsInsightProvider()),
        )
        val callContext = ManagerCrmInsightContext(salonId = "s1", customers = customers, appointments = emptyList())

        val first = composite.insightsFor(callContext).map { it.id }
        val second = composite.insightsFor(callContext).map { it.id }

        assertEquals(first, second)
        assertEquals(listOf("vip-customer-c1", "vip-customer-c2", "vip-without-appointments-c1", "vip-without-appointments-c2"), first)
    }
}
