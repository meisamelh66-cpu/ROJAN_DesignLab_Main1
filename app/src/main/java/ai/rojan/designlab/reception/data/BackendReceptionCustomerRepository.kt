package ai.rojan.designlab.reception.data

import ai.rojan.designlab.data.remote.ManagerCustomerApi
import ai.rojan.designlab.data.remote.dto.CustomerIdentityResponseDto
import ai.rojan.designlab.data.remote.dto.CustomerResponseDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.domain.repository.PagedResult
import ai.rojan.designlab.reception.domain.repository.ReceptionCustomer
import ai.rojan.designlab.reception.domain.repository.ReceptionCustomerRepository

/**
 * Real backend-backed [ReceptionCustomerRepository] — reuses the existing
 * [ManagerCustomerApi] Retrofit interface as-is (a plain contract for
 * `CustomerController`, not flavor-restricted despite its name).
 *
 * Gap P0-3-REC-IDENTITY-01: [listCustomers] calls the narrower, Reception-eligible
 * `GET .../customers/identity` ([ManagerCustomerApi.searchIdentity],
 * `Permission.VIEW_CUSTOMER_IDENTITY`/`VIEW_CRM` on the backend) — never the owner-only
 * `GET .../customers` ([ManagerCustomerApi.list], `VIEW_CRM`-only) a real `RECEPTIONIST`
 * caller would get a 403 from. [getCustomer] still calls the owner-only
 * `GET .../customers/{id}` — the backend has no identity-scoped single-customer endpoint
 * yet (a separate, smaller follow-on gap, not this one) — `create`/`update`/`notes`/
 * `bookings` on [ManagerCustomerApi] remain deliberately never invoked here, matching
 * [ReceptionCustomerRepository]'s identity-only scope (System 1 decision §1c /
 * `ROJAN_Reception_Permission_Contract_Update_ADR_v1.md`).
 */
class BackendReceptionCustomerRepository(
    private val managerCustomerApi: ManagerCustomerApi,
) : ReceptionCustomerRepository {

    override suspend fun listCustomers(
        salonId: String,
        search: String?,
        page: Int,
        size: Int,
    ): Result<PagedResult<ReceptionCustomer>> =
        safeApiCall { managerCustomerApi.searchIdentity(salonId = salonId, page = page, size = size, search = search) }
            .map { dto ->
                PagedResult(
                    content = dto.content.map { it.toDomain() },
                    page = dto.page,
                    size = dto.size,
                    totalElements = dto.totalElements,
                    totalPages = dto.totalPages,
                )
            }

    override suspend fun getCustomer(salonId: String, customerId: String): Result<ReceptionCustomer> =
        safeApiCall { managerCustomerApi.get(salonId = salonId, customerId = customerId) }.map { it.toDomain() }

    private fun CustomerIdentityResponseDto.toDomain() = ReceptionCustomer(
        id = id,
        salonId = salonId,
        fullName = fullName,
        phoneNumber = phoneNumber,
        email = email,
        active = active,
    )

    private fun CustomerResponseDto.toDomain() = ReceptionCustomer(
        id = id,
        salonId = salonId,
        fullName = fullName,
        phoneNumber = phoneNumber,
        email = email,
        active = active,
    )
}
