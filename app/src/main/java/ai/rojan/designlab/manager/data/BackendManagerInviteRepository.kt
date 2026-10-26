package ai.rojan.designlab.manager.data

import ai.rojan.designlab.data.remote.ManagerInviteApi
import ai.rojan.designlab.data.remote.dto.CreateSalonInviteRequestDto
import ai.rojan.designlab.data.remote.dto.NetworkSalonInviteStatus
import ai.rojan.designlab.data.remote.dto.NetworkSalonRole
import ai.rojan.designlab.data.remote.dto.SalonInviteResponseDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.manager.domain.membership.SalonMemberRole
import ai.rojan.designlab.manager.domain.repository.ManagerInviteRepository
import ai.rojan.designlab.manager.domain.repository.SalonInvite
import ai.rojan.designlab.manager.domain.repository.SalonInviteStatus

/** Real backend-backed [ManagerInviteRepository] — see that interface's own doc comment for the confirmed `SalonInviteController` contract this implements. [salonId] is baked in at construction, same convention [BackendSpecialistRepository] already established. */
class BackendManagerInviteRepository(
    private val managerInviteApi: ManagerInviteApi,
    private val salonId: String,
) : ManagerInviteRepository {

    override suspend fun issueInvite(role: SalonMemberRole): Result<SalonInvite> =
        safeApiCall {
            managerInviteApi.createInvite(salonId, CreateSalonInviteRequestDto(role = role.toNetwork()))
        }.map { it.toDomain() }

    override suspend fun listInvites(): Result<List<SalonInvite>> =
        safeApiCall { managerInviteApi.listInvites(salonId) }.map { list -> list.map { it.toDomain() } }

    override suspend fun revokeInvite(inviteId: String): Result<Unit> =
        safeApiCall { managerInviteApi.revokeInvite(salonId, inviteId) }

    override suspend fun getInviteQrCode(inviteId: String, sizePx: Int): Result<ByteArray> =
        safeApiCall { managerInviteApi.getQrCode(salonId, inviteId, sizePx).bytes() }

    private fun SalonMemberRole.toNetwork() = when (this) {
        SalonMemberRole.MANAGER -> NetworkSalonRole.MANAGER
        SalonMemberRole.RECEPTIONIST -> NetworkSalonRole.RECEPTIONIST
    }

    private fun NetworkSalonRole.toDomain() = when (this) {
        NetworkSalonRole.MANAGER -> SalonMemberRole.MANAGER
        NetworkSalonRole.RECEPTIONIST -> SalonMemberRole.RECEPTIONIST
    }

    private fun NetworkSalonInviteStatus.toDomain() = when (this) {
        NetworkSalonInviteStatus.CREATED -> SalonInviteStatus.CREATED
        NetworkSalonInviteStatus.ACCEPTED -> SalonInviteStatus.ACCEPTED
        NetworkSalonInviteStatus.EXPIRED -> SalonInviteStatus.EXPIRED
        NetworkSalonInviteStatus.REVOKED -> SalonInviteStatus.REVOKED
    }

    private fun SalonInviteResponseDto.toDomain() = SalonInvite(
        id = id,
        salonId = salonId,
        role = role.toDomain(),
        status = status.toDomain(),
        expiresAt = expiresAt,
        createdAt = createdAt,
    )
}
