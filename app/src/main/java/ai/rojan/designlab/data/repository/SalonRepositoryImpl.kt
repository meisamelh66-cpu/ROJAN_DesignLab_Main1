package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.SalonApi
import ai.rojan.designlab.data.remote.dto.SalonResponseDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.domain.repository.PagedResult
import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.domain.repository.SalonRepository

class SalonRepositoryImpl(
    private val salonApi: SalonApi,
) : SalonRepository {

    override suspend fun browseSalons(page: Int, size: Int, nameFilter: String?): Result<PagedResult<Salon>> =
        safeApiCall { salonApi.browseSalons(page = page, size = size, name = nameFilter?.takeIf { it.isNotBlank() }) }
            .map { dto ->
                PagedResult(
                    content = dto.content.map { it.toDomain() },
                    page = dto.page,
                    size = dto.size,
                    totalElements = dto.totalElements,
                    totalPages = dto.totalPages,
                )
            }

    override suspend fun getSalon(salonId: String): Result<Salon> =
        safeApiCall { salonApi.getSalon(salonId) }.map { it.toDomain() }

    private fun SalonResponseDto.toDomain() = Salon(
        id = id,
        name = name,
        description = description,
        phone = phone,
        email = email,
        address = address,
    )
}
