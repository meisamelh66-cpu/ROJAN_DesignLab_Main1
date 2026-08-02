package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.ServiceApi
import ai.rojan.designlab.data.remote.dto.ServiceResponseDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.domain.repository.Service
import ai.rojan.designlab.domain.repository.ServiceRepository

class ServiceRepositoryImpl(
    private val serviceApi: ServiceApi,
) : ServiceRepository {

    override suspend fun getServices(salonId: String, categoryId: String): Result<List<Service>> =
        safeApiCall { serviceApi.getServices(salonId, categoryId) }.map { list -> list.map { it.toDomain() } }

    private fun ServiceResponseDto.toDomain() = Service(
        id = id,
        salonId = salonId,
        categoryId = categoryId,
        name = name,
        description = description,
        durationMinutes = durationMinutes,
        price = price,
    )
}
