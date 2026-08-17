package ai.rojan.designlab.manager.data

import ai.rojan.designlab.data.remote.ManagerSalonApi
import ai.rojan.designlab.data.remote.dto.CreateSalonRequestDto
import ai.rojan.designlab.data.remote.dto.SalonResponseDto
import ai.rojan.designlab.data.remote.dto.UpdateSalonRequestDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.manager.domain.dashboard.ManagerSalonSummary
import ai.rojan.designlab.manager.domain.repository.ManagerSalonRepository

/**
 * Real backend-backed [ManagerSalonRepository] (Phase A — Owner Salon
 * Identity). No cache, unlike [BackendSpecialistRepository] et al. — this
 * repository backs exactly one screen
 * ([ai.rojan.designlab.manager.screens.settings.ManagerSalonSetupScreen])
 * with no list to keep in sync elsewhere, so each call talks straight to
 * the backend, same reasoning [BackendSalonMembershipRepository] already
 * documents for the same "no consuming list yet" shape.
 */
class BackendManagerSalonRepository(
    private val managerSalonApi: ManagerSalonApi,
) : ManagerSalonRepository {

    override suspend fun getMySalon(): Result<ManagerSalonSummary?> =
        safeApiCall { managerSalonApi.mine() }.map { salons -> salons.firstOrNull()?.toDomain() }

    override suspend fun createSalon(
        name: String,
        description: String?,
        phone: String,
        email: String?,
        address: String,
    ): Result<ManagerSalonSummary> =
        safeApiCall {
            managerSalonApi.create(
                CreateSalonRequestDto(
                    name = name,
                    description = description,
                    phone = phone,
                    email = email,
                    address = address,
                ),
            )
        }.map { it.toDomain() }

    override suspend fun updateSalon(
        salonId: String,
        name: String,
        description: String?,
        phone: String,
        email: String?,
        address: String,
        latitude: Double?,
        longitude: Double?,
    ): Result<ManagerSalonSummary> =
        safeApiCall {
            managerSalonApi.update(
                salonId = salonId,
                request = UpdateSalonRequestDto(
                    name = name,
                    description = description,
                    phone = phone,
                    email = email,
                    address = address,
                    latitude = latitude,
                    longitude = longitude,
                ),
            )
        }.map { it.toDomain() }

    private fun SalonResponseDto.toDomain() = ManagerSalonSummary(
        id = id,
        name = name,
        description = description,
        phone = phone,
        email = email,
        address = address,
        latitude = latitude,
        longitude = longitude,
        active = active,
        logoUrl = logoUrl,
        coverImageUrl = coverImageUrl,
    )
}
