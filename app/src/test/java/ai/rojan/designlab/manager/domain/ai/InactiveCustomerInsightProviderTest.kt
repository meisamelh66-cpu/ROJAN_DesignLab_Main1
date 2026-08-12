package ai.rojan.designlab.manager.domain.ai

import ai.rojan.designlab.manager.domain.customer.CustomerTag
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Manager CRM AI Foundation, Phase 7 Step 3 — hermetic (no network, no
 * Android framework) coverage of [InactiveCustomerInsightProvider], a
 * pure function over an already-fetched [ManagerCustomer] list.
 */
class InactiveCustomerInsightProviderTest {

    private val provider = InactiveCustomerInsightProvider()

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
    fun `no inactive customers yields no insights`() {
        val customers = listOf(
            customer("c1", CustomerTag.VIP),
            customer("c2", CustomerTag.NEW),
            customer("c3", CustomerTag.REGULAR),
        )

        val insights = provider.insightsFor(ManagerCrmInsightContext(salonId = "s1", customers = customers))

        assertTrue(insights.isEmpty())
    }

    @Test
    fun `a mix of tags surfaces only the inactive customers`() {
        val customers = listOf(
            customer("c1", CustomerTag.VIP),
            customer("c2", CustomerTag.INACTIVE),
            customer("c3", CustomerTag.NEW),
            customer("c4", CustomerTag.INACTIVE),
            customer("c5", CustomerTag.REGULAR),
        )

        val insights = provider.insightsFor(ManagerCrmInsightContext(salonId = "s1", customers = customers))

        assertEquals(2, insights.size)
        assertEquals(setOf("c2", "c4"), insights.map { it.id.removePrefix("inactive-customer-") }.toSet())
        assertTrue(insights.all { it.category == ManagerCrmInsightCategory.INACTIVE_CUSTOMER })
    }

    @Test
    fun `every customer inactive surfaces every customer`() {
        val customers = listOf(
            customer("c1", CustomerTag.INACTIVE),
            customer("c2", CustomerTag.INACTIVE),
            customer("c3", CustomerTag.INACTIVE),
        )

        val insights = provider.insightsFor(ManagerCrmInsightContext(salonId = "s1", customers = customers))

        assertEquals(3, insights.size)
    }

    @Test
    fun `each insight id is stable and traceable back to the source customer id`() {
        val customers = listOf(customer("c-42", CustomerTag.INACTIVE, name = "Sara"))

        val first = provider.insightsFor(ManagerCrmInsightContext(salonId = "s1", customers = customers))
        val second = provider.insightsFor(ManagerCrmInsightContext(salonId = "s1", customers = customers))

        assertEquals("inactive-customer-c-42", first.single().id)
        assertEquals(first.single().id, second.single().id)
        assertEquals("Sara", first.single().title)
    }

    @Test
    fun `the reason never claims a duration or cause`() {
        val customers = listOf(customer("c1", CustomerTag.INACTIVE))

        val reason = provider.insightsFor(ManagerCrmInsightContext(salonId = "s1", customers = customers)).single().reason

        assertTrue(reason.none { it.isDigit() })
    }
}
