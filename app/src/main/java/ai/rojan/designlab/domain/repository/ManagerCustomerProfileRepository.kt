package ai.rojan.designlab.domain.repository

import ai.rojan.designlab.manager.domain.customer.ManagerCustomerProfile

/**
 * FIX-006. Loads a salon customer's full CRM profile from the backend
 * Customer CRM (`GET /customer-records/{id}` + `/notes` + `/tags` +
 * `/timeline` + `/bookings`), owner-scoped.
 *
 * The Customers list navigates with a legacy roster id (a linked account's
 * `UserId`), while the CRM detail endpoints are keyed on the salon's own
 * `CustomerId`; [loadProfileByAccountId] resolves the former to the latter
 * against the salon's `/customer-records` list. `Result.success(null)`
 * means the account has no CRM record for this salon (it never booked
 * here).
 */
interface ManagerCustomerProfileRepository {
    suspend fun loadProfileByAccountId(salonId: String, accountId: String): Result<ManagerCustomerProfile?>
}
