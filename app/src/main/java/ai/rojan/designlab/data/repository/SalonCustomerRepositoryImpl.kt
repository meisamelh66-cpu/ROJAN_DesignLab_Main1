package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.SalonCustomerApi
import ai.rojan.designlab.data.remote.dto.CustomerResponseDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.domain.repository.SalonCustomer
import ai.rojan.designlab.domain.repository.SalonCustomerRepository

class SalonCustomerRepositoryImpl(
    private val salonCustomerApi: SalonCustomerApi,
) : SalonCustomerRepository {

    override suspend fun searchCustomers(salonId: String, query: String?): Result<List<SalonCustomer>> =
        safeApiCall { salonCustomerApi.search(salonId = salonId, query = query?.takeIf { it.isNotBlank() }) }
            .map { paged -> paged.content.map { it.toDomain() } }

    // email passes through nullable — see SalonCustomer's own doc comment;
    // a phone-only OTP account may genuinely have none on file.
    private fun CustomerResponseDto.toDomain() = SalonCustomer(id = id, email = email, fullName = fullName)
}
