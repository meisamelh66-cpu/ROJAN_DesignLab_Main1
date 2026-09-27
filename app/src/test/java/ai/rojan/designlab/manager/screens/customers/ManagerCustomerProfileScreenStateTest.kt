package ai.rojan.designlab.manager.screens.customers

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.data.remote.NetworkUnavailableException
import ai.rojan.designlab.manager.domain.customer.CustomerTag
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * P0 Safety Fix (Manager Completeness Audit v1, finding P0-1) — covers
 * [resolveCustomerProfileState], the pure function [ManagerCustomerProfileScreen]
 * now uses instead of the old `getById(customerId) ?: getAll().firstOrNull()`
 * fallback that could silently substitute a different customer.
 */
class ManagerCustomerProfileScreenStateTest {

    private val requestedCustomer = ManagerCustomer(
        id = "requested-id",
        name = "سارا احمدی",
        phone = "09120000000",
        tag = CustomerTag.REGULAR,
        loyaltyScore = 10,
        notes = null,
        lastVisit = "—",
        totalVisits = 3,
    )

    private val otherCustomer = ManagerCustomer(
        id = "some-other-id",
        name = "مریم رضایی",
        phone = "09121111111",
        tag = CustomerTag.VIP,
        loyaltyScore = 50,
        notes = null,
        lastVisit = "—",
        totalVisits = 20,
    )

    @Test
    fun `success plus resolved customer produces Found with that exact customer`() {
        val state = resolveCustomerProfileState(
            loadDetailResult = Result.success(Unit),
            resolvedCustomer = requestedCustomer,
        )

        assertEquals(CustomerProfileLookupState.Found(requestedCustomer), state)
    }

    @Test
    fun `success plus no resolved customer produces NotFound - never substitutes another customer`() {
        val state = resolveCustomerProfileState(
            loadDetailResult = Result.success(Unit),
            resolvedCustomer = null,
        )

        assertEquals(CustomerProfileLookupState.NotFound, state)
        // The regression this guards against: the old code fell back to
        // `getAll().firstOrNull()` here. This function has no access to any
        // customer list at all - only the single already-resolved candidate -
        // so there is no way for it to ever return `otherCustomer`.
        assertTrue(state !is CustomerProfileLookupState.Found)
    }

    @Test
    fun `a repository failure is always Error, never silently reinterpreted as NotFound`() {
        val networkFailure = Result.failure<Unit>(NetworkUnavailableException(IOException("offline")))

        val state = resolveCustomerProfileState(
            loadDetailResult = networkFailure,
            // Even when a customer WOULD have resolved locally, a genuine fetch
            // failure must still win - the user needs to see a real error/retry,
            // not a happy-path profile built on an incomplete/failed load.
            resolvedCustomer = requestedCustomer,
        )

        assertTrue(state is CustomerProfileLookupState.Error)
        assertTrue((state as CustomerProfileLookupState.Error).message.isNotBlank())
    }

    @Test
    fun `a backend error failure also produces Error, distinct from NotFound`() {
        val backendFailure = Result.failure<Unit>(BackendApiException(statusCode = 500, apiError = null))

        val state = resolveCustomerProfileState(
            loadDetailResult = backendFailure,
            resolvedCustomer = null,
        )

        assertTrue("A real server error must never be shown as a plain 'not found'", state is CustomerProfileLookupState.Error)
    }

    @Test
    fun `found never wraps a customer other than the one explicitly passed in`() {
        val state = resolveCustomerProfileState(
            loadDetailResult = Result.success(Unit),
            resolvedCustomer = requestedCustomer,
        )

        val found = state as CustomerProfileLookupState.Found
        assertEquals(requestedCustomer.id, found.customer.id)
        assertTrue(found.customer.id != otherCustomer.id)
    }
}
