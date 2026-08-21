package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.SpecialistApi
import ai.rojan.designlab.data.remote.dto.SpecialistResponseDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.domain.repository.Specialist
import ai.rojan.designlab.domain.repository.SpecialistRepository

class SpecialistRepositoryImpl(
    private val specialistApi: SpecialistApi,
) : SpecialistRepository {

    override suspend fun getSpecialists(salonId: String): Result<List<Specialist>> =
        safeApiCall { specialistApi.getSpecialists(salonId) }.map { list -> list.map { it.toDomain() } }

    override suspend fun getSpecialist(salonId: String, specialistId: String): Result<Specialist> =
        safeApiCall { specialistApi.getSpecialist(salonId, specialistId) }.map { it.toDomain() }

    override suspend fun getPortfolio(salonId: String, specialistId: String): Result<List<String>> =
        safeApiCall { specialistApi.getMedia(salonId, mediaType = "PORTFOLIO", targetId = specialistId) }
            .map { assets -> assets.map { it.url } }

    private fun SpecialistResponseDto.toDomain() = Specialist(
        id = id,
        salonId = salonId,
        displayName = displayName,
        bio = bio,
        photoUrl = photoUrl,
    )
}
