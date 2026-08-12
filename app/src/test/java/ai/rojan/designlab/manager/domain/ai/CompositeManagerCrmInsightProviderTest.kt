package ai.rojan.designlab.manager.domain.ai

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
 * [InactiveCustomerInsightProviderTest]/[VipCustomerInsightProviderTest]).
 */
class CompositeManagerCrmInsightProviderTest {

    private fun insight(id: String, category: ManagerCrmInsightCategory) =
        ManagerCrmInsight(id = id, category = category, title = "t-$id", reason = "r-$id")

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
        assertEquals("vip-customer-c2", combined[1].id)
    }
}
