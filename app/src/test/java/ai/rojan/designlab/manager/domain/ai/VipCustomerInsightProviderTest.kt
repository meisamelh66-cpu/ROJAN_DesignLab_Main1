package ai.rojan.designlab.manager.domain.ai

import ai.rojan.designlab.manager.domain.customer.CustomerTag
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Manager CRM AI Foundation, Phase 7 Step 5 — hermetic coverage of
 * [VipCustomerInsightProvider], mirroring
 * [InactiveCustomerInsightProviderTest]'s shape for the sibling rule.
 */
class VipCustomerInsightProviderTest {

    private val provider = VipCustomerInsightProvider()

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

    @Test
    fun `zero customers yields no insights`() {
        val insights = provider.insightsFor(ManagerCrmInsightContext(salonId = "s1", customers = emptyList()))

        assertTrue(insights.isEmpty())
    }

    @Test
    fun `a mix of tags surfaces only the VIP customers`() {
        val customers = listOf(
            customer("c1", CustomerTag.VIP),
            customer("c2", CustomerTag.INACTIVE),
            customer("c3", CustomerTag.NEW),
            customer("c4", CustomerTag.VIP),
            customer("c5", CustomerTag.REGULAR),
        )

        val insights = provider.insightsFor(ManagerCrmInsightContext(salonId = "s1", customers = customers))

        assertEquals(2, insights.size)
        assertEquals(setOf("c1", "c4"), insights.map { it.id.removePrefix("vip-customer-") }.toSet())
        assertEquals(setOf("c1", "c4"), insights.map { it.customerId }.toSet())
        assertTrue(insights.all { it.category == ManagerCrmInsightCategory.VIP_CUSTOMER })
    }

    @Test
    fun `each insight id is stable and traceable back to the source customer id`() {
        val customers = listOf(customer("c-7", CustomerTag.VIP, name = "Reza"))

        val first = provider.insightsFor(ManagerCrmInsightContext(salonId = "s1", customers = customers))
        val second = provider.insightsFor(ManagerCrmInsightContext(salonId = "s1", customers = customers))

        assertEquals("vip-customer-c-7", first.single().id)
        assertEquals(first.single().id, second.single().id)
        assertEquals("c-7", first.single().customerId)
        assertEquals(first.single().customerId, second.single().customerId)
        assertEquals("Reza", first.single().title)
    }

    @Test
    fun `the reason never claims a duration, value, or recommendation`() {
        val customers = listOf(customer("c1", CustomerTag.VIP))

        val reason = provider.insightsFor(ManagerCrmInsightContext(salonId = "s1", customers = customers)).single().reason

        assertTrue(reason.none { it.isDigit() })
    }
}
