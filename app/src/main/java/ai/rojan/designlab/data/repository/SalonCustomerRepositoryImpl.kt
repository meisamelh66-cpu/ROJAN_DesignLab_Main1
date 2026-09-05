package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.SalonCustomerApi
import ai.rojan.designlab.data.remote.dto.UserResponseDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.domain.repository.SalonCustomer
import ai.rojan.designlab.domain.repository.SalonCustomerRepository

class SalonCustomerRepositoryImpl(
    private val salonCustomerApi: SalonCustomerApi,
) : SalonCustomerRepository {

    override suspend fun searchCustomers(salonId: String, query: String?): Result<List<SalonCustomer>> =
        safeApiCall { salonCustomerApi.search(salonId = salonId, query = query?.takeIf { it.isNotBlank() }) }
            .map { list -> list.map { it.toDomain() } }

    private fun UserResponseDto.toDomain() = SalonCustomer(id = id, email = email, fullName = fullName)
}
