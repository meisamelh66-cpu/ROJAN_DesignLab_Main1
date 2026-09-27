package ai.rojan.designlab.manager.screens.customers

import ai.rojan.designlab.manager.domain.customer.CustomerTag
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase F1-A Safety Fix (Phase E audit finding) — covers [resolveCustomerEditLookupState], the pure
 * function [ManagerCustomerEditScreen] now uses instead of silently `return@ManagerScaffold`-ing a
 * blank screen when the requested `customerId` has no local record.
 */
class ManagerCustomerEditScreenStateTest {

    private val customer = ManagerCustomer(
        id = "customer-1",
        name = "سارا احمدی",
        phone = "09120000000",
        tag = CustomerTag.REGULAR,
        loyaltyScore = 0,
        notes = null,
        lastVisit = "—",
        totalVisits = 3,
    )

    @Test
    fun `a null lookup is NotFound, never a blank success`() {
        val state = resolveCustomerEditLookupState(null)

        assertEquals(CustomerEditLookupState.NotFound, state)
        assertTrue(state !is CustomerEditLookupState.Found)
    }

    @Test
    fun `a resolved customer is Found wrapping that exact record`() {
        val state = resolveCustomerEditLookupState(customer)

        assertEquals(CustomerEditLookupState.Found(customer), state)
    }
}
