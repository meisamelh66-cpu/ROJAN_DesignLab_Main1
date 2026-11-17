package ai.rojan.designlab.manager.data

import ai.rojan.designlab.data.remote.ManagerInviteApi
import ai.rojan.designlab.data.remote.dto.CreateSalonInviteRequestDto
import ai.rojan.designlab.data.remote.dto.SalonInviteResponseDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.manager.domain.membership.SalonMemberRole
import ai.rojan.designlab.manager.domain.repository.ManagerInviteRepository
import ai.rojan.designlab.manager.domain.repository.SalonInvite
import ai.rojan.designlab.manager.domain.repository.SalonInviteStatus

/**
 * Real backend-backed [ManagerInviteRepository] - no cache, same reasoning
 * [BackendManagerWorkingHoursRepository] already documents: this repository
 * backs exactly one screen with no list to keep in sync elsewhere.
 */
class BackendManagerInviteRepository(
    private val inviteApi: ManagerInviteApi,
) : ManagerInviteRepository {

    override suspend fun issueInvite(salonId: String, role: SalonMemberRole): Result<SalonInvite> =
        safeApiCall { inviteApi.create(salonId, CreateSalonInviteRequestDto(role = role.name)) }.map { it.toDomain() }

    private fun SalonInviteResponseDto.toDomain() = SalonInvite(
        id = id,
        salonId = salonId,
        role = SalonMemberRole.valueOf(role),
        token = token,
        status = SalonInviteStatus.valueOf(status),
        expiresAt = expiresAt,
    )
}
