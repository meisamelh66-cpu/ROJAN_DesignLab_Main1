package ai.rojan.designlab.reception.data

import ai.rojan.designlab.data.remote.ManagerCustomerApi
import ai.rojan.designlab.data.remote.dto.CustomerResponseDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.domain.repository.PagedResult
import ai.rojan.designlab.reception.domain.repository.ReceptionCustomer
import ai.rojan.designlab.reception.domain.repository.ReceptionCustomerRepository

/**
 * Real backend-backed [ReceptionCustomerRepository] — reuses the existing
 * [ManagerCustomerApi] Retrofit interface as-is (a plain contract for
 * `CustomerController`, not flavor-restricted despite its name). Only
 * `list`/`get` are called — `create`/`update`/`notes`/`bookings` on
 * [ManagerCustomerApi] are deliberately never invoked here, matching
 * [ReceptionCustomerRepository]'s `VIEW_CRM`-only scope (System 1 decision
 * §1c) — this class has no path to a `MANAGE_CRM` action even though the
 * underlying API interface exposes one.
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
        safeApiCall { managerCustomerApi.list(salonId = salonId, page = page, size = size, search = search) }
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

    private fun CustomerResponseDto.toDomain() = ReceptionCustomer(
        id = id,
        salonId = salonId,
        fullName = fullName,
        phoneNumber = phoneNumber,
        email = email,
        active = active,
    )
}
