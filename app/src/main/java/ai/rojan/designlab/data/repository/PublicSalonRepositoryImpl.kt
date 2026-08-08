package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.PublicSalonApi
import ai.rojan.designlab.data.remote.dto.PublicSalonResponseDto
import ai.rojan.designlab.data.remote.dto.PublicServiceCategoryResponseDto
import ai.rojan.designlab.data.remote.dto.PublicServiceResponseDto
import ai.rojan.designlab.data.remote.dto.PublicSpecialistResponseDto
import ai.rojan.designlab.data.remote.dto.TimeSlotResponseDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.domain.repository.PublicSalon
import ai.rojan.designlab.domain.repository.PublicService
import ai.rojan.designlab.domain.repository.PublicServiceCategory
import ai.rojan.designlab.domain.repository.PublicSalonRepository
import ai.rojan.designlab.domain.repository.PublicSpecialist
import ai.rojan.designlab.domain.repository.TimeSlot

class PublicSalonRepositoryImpl(
    private val publicSalonApi: PublicSalonApi,
) : PublicSalonRepository {

    override suspend fun getSalon(slug: String): Result<PublicSalon> =
        safeApiCall { publicSalonApi.getSalon(slug) }.map { it.toDomain() }

    override suspend fun getCategories(slug: String): Result<List<PublicServiceCategory>> =
        safeApiCall { publicSalonApi.getCategories(slug) }.map { list -> list.map { it.toDomain() } }

    override suspend fun getServices(slug: String, categoryId: String): Result<List<PublicService>> =
        safeApiCall { publicSalonApi.getServices(slug, categoryId) }.map { list -> list.map { it.toDomain() } }

    override suspend fun getSpecialists(slug: String): Result<List<PublicSpecialist>> =
        safeApiCall { publicSalonApi.getSpecialists(slug) }.map { list -> list.map { it.toDomain() } }

    override suspend fun getAvailableSlots(
        slug: String,
        specialistId: String,
        serviceId: String,
        date: String,
        slotIntervalMinutes: Int,
    ): Result<List<TimeSlot>> =
        safeApiCall {
            publicSalonApi.getAvailableSlots(
                slug = slug,
                specialistId = specialistId,
                serviceId = serviceId,
                date = date,
                slotIntervalMinutes = slotIntervalMinutes,
            )
        }.map { list -> list.map { it.toDomain() } }

    private fun PublicSalonResponseDto.toDomain() = PublicSalon(
        id = id,
        name = name,
        description = description,
        phone = phone,
        address = address,
        logoUrl = logoUrl,
        latitude = latitude,
        longitude = longitude,
    )

    private fun PublicServiceCategoryResponseDto.toDomain() = PublicServiceCategory(
        id = id,
        name = name,
        description = description,
    )

    private fun PublicServiceResponseDto.toDomain() = PublicService(
        id = id,
        categoryId = categoryId,
        name = name,
        description = description,
        durationMinutes = durationMinutes,
        price = price,
    )

    private fun PublicSpecialistResponseDto.toDomain() = PublicSpecialist(
        id = id,
        displayName = displayName,
        bio = bio,
        photoUrl = photoUrl,
    )

    private fun TimeSlotResponseDto.toDomain() = TimeSlot(start = start, end = end)
}
