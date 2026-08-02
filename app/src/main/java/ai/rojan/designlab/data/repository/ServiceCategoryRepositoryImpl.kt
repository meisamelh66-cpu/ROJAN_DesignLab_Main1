package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.ServiceCategoryApi
import ai.rojan.designlab.data.remote.dto.ServiceCategoryResponseDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.domain.repository.ServiceCategory
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository

class ServiceCategoryRepositoryImpl(
    private val serviceCategoryApi: ServiceCategoryApi,
) : ServiceCategoryRepository {

    override suspend fun getCategories(salonId: String): Result<List<ServiceCategory>> =
        safeApiCall { serviceCategoryApi.getCategories(salonId) }.map { list -> list.map { it.toDomain() } }

    private fun ServiceCategoryResponseDto.toDomain() = ServiceCategory(
        id = id,
        salonId = salonId,
        name = name,
        description = description,
    )
}
