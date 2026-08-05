package ai.rojan.designlab.manager.data

import ai.rojan.designlab.data.remote.ManagerCustomerApi
import ai.rojan.designlab.data.remote.dto.CreateCustomerRequestDto
import ai.rojan.designlab.data.remote.dto.CustomerResponseDto
import ai.rojan.designlab.data.remote.dto.NetworkCustomerStatus
import ai.rojan.designlab.data.remote.dto.UpdateCustomerRequestDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.manager.domain.customer.CustomerServiceHistoryEntry
import ai.rojan.designlab.manager.domain.customer.CustomerTag
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import ai.rojan.designlab.manager.domain.repository.CustomerRepository

/**
 * Real backend-backed [CustomerRepository] (Final Backend Integration —
 * Customer Module). `getAll()`/`getById()`/`search()` read an in-memory
 * cache populated by [sync], sourced from the real, owner-authenticated
 * `GET /api/v1/salons/{salonId}/customers` (`CustomerController`) — same
 * cache-then-serve-synchronously shape as [BackendServiceRepository]/
 * [BackendAppointmentRepository], for the same reason: existing call
 * sites (`ManagerBookingViewModel`, `ManagerCalendarScreen`,
 * `ManagerCustomerProfileScreen`, `ManagerCustomersListScreen`) read these
 * synchronously and weren't rewritten to `suspend` for this pass.
 *
 * `create`/`update` call the real `POST`/`PATCH` endpoints — neither has
 * an existing caller (verified by grep), so making them `suspend` here is
 * a zero-risk interface change; implemented against the real controller so
 * whichever screen eventually adds customer creation/editing has something
 * real to call immediately.
 *
 * **Fields with no real backend equivalent, mapped to honest "no data"
 * defaults, not fabricated:** [ManagerCustomer.loyaltyScore] (the backend
 * has `lifetimeValue`, a monetary total, not a 0-100 score — see
 * `CustomerResponseDto`'s own doc comment), [ManagerCustomer.lastVisit] and
 * [ManagerCustomer.totalVisits] (no visit-count/last-visit field anywhere
 * in `CustomerResponse`; the backend's booking-per-customer data lives
 * behind a separate, out-of-scope-for-this-phase endpoint,
 * `GET .../customers/{id}/bookings`).
 *
 * [getServiceHistory] returns an empty list for every customer — no
 * backend endpoint for this was in this phase's scope (the closest is the
 * same out-of-scope `.../bookings` endpoint above, which doesn't carry
 * specialist/price in the shape [CustomerServiceHistoryEntry] needs
 * without further mapping this pass doesn't assume).
 */
class BackendCustomerRepository(
    private val managerCustomerApi: ManagerCustomerApi,
    private val salonId: String,
) : CustomerRepository {

    private var cache: List<ManagerCustomer> = emptyList()

    /** Fetches this salon's customers from the backend and repopulates the cache. Call before first read, and to refresh. */
    suspend fun sync(): Result<Unit> = safeApiCall {
        managerCustomerApi.list(salonId, page = 0, size = 200)
    }.map { paged ->
        cache = paged.content.map { it.toDomain() }
    }

    override fun getAll(): List<ManagerCustomer> = cache

    override fun getById(id: String): ManagerCustomer? = cache.find { it.id == id }

    override fun search(query: String): List<ManagerCustomer> =
        if (query.isBlank()) {
            cache
        } else {
            cache.filter { it.name.contains(query, ignoreCase = true) || it.phone.contains(query) }
        }

    override suspend fun create(customer: ManagerCustomer): Result<ManagerCustomer> =
        safeApiCall {
            managerCustomerApi.create(
                salonId = salonId,
                request = CreateCustomerRequestDto(
                    fullName = customer.name,
                    phoneNumber = customer.phone.ifBlank { null },
                ),
            )
        }.map { dto ->
            dto.toDomain().also { created -> cache = cache + created }
        }

    override suspend fun update(customer: ManagerCustomer): Result<ManagerCustomer?> =
        safeApiCall {
            managerCustomerApi.update(
                salonId = salonId,
                customerId = customer.id,
                request = UpdateCustomerRequestDto(
                    fullName = customer.name,
                    phoneNumber = customer.phone.ifBlank { null },
                    status = customer.tag.toNetworkStatus(),
                ),
            )
        }.map { dto ->
            dto.toDomain().also { updated ->
                cache = cache.map { if (it.id == updated.id) updated else it }
            }
        }

    /** No backend endpoint in this phase's scope — see class doc comment. */
    override fun getServiceHistory(customerId: String): List<CustomerServiceHistoryEntry> = emptyList()

    private fun CustomerResponseDto.toDomain() = ManagerCustomer(
        id = id,
        name = fullName,
        phone = phoneNumber.orEmpty(),
        tag = status.toDomainTag(),
        loyaltyScore = 0,
        notes = null,
        lastVisit = "—",
        totalVisits = 0,
    )

    /** 6 backend statuses -> 4 domain tags — see [CustomerResponseDto]'s doc comment on why these are richer than the mobile app's own model. */
    private fun NetworkCustomerStatus.toDomainTag(): CustomerTag = when (this) {
        NetworkCustomerStatus.VIP -> CustomerTag.VIP
        NetworkCustomerStatus.INACTIVE, NetworkCustomerStatus.CHURNED -> CustomerTag.INACTIVE
        NetworkCustomerStatus.LEAD, NetworkCustomerStatus.PROSPECT -> CustomerTag.NEW
        NetworkCustomerStatus.ACTIVE -> CustomerTag.REGULAR
    }

    /** Inverse of [toDomainTag] for writes — lossy (4 -> 6), picks the nearest real status rather than inventing a new one. */
    private fun CustomerTag.toNetworkStatus(): NetworkCustomerStatus = when (this) {
        CustomerTag.VIP -> NetworkCustomerStatus.VIP
        CustomerTag.INACTIVE -> NetworkCustomerStatus.INACTIVE
        CustomerTag.NEW -> NetworkCustomerStatus.LEAD
        CustomerTag.REGULAR -> NetworkCustomerStatus.ACTIVE
    }
}
