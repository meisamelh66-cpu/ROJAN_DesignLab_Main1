package ai.rojan.designlab.reception.domain.repository

import ai.rojan.designlab.domain.repository.PagedResult

/**
 * Read-only customer identity — deliberately narrower than Manager's own
 * `ManagerCustomer` (no lifetime value, no note/tag mutation surface):
 * per `ROJAN_System1_Backend_Decision_v2.md` §1c, `RECEPTIONIST` membership
 * is fixed at `VIEW_CRM`, not `MANAGE_CRM` — this type and
 * [ReceptionCustomerRepository] intentionally expose only what a
 * `VIEW_CRM`-scoped caller is meant to see/do.
 */
data class ReceptionCustomer(
    val id: String,
    val salonId: String,
    val fullName: String,
    val phoneNumber: String?,
    val email: String?,
    val active: Boolean,
)

/**
 * Backed by the real, already-existing `CustomerController`
 * (`GET /api/v1/salons/{salonId}/customers`, `.../{customerId}`) — owner-only
 * today, same "will legitimately fail until authorization broadening
 * ships" status as [ReceptionBookingRepository]. No mock, no fake data.
 */
interface ReceptionCustomerRepository {

    suspend fun listCustomers(
        salonId: String,
        search: String? = null,
        page: Int = 0,
        size: Int = 100,
    ): Result<PagedResult<ReceptionCustomer>>

    suspend fun getCustomer(salonId: String, customerId: String): Result<ReceptionCustomer>
}
