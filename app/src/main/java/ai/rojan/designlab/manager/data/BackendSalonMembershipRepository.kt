package ai.rojan.designlab.manager.data

import ai.rojan.designlab.data.remote.SalonMembershipApi
import ai.rojan.designlab.data.remote.dto.AssignMembershipRequestDto
import ai.rojan.designlab.data.remote.dto.NetworkSalonRole
import ai.rojan.designlab.data.remote.dto.SalonMembershipResponseDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.manager.domain.membership.SalonMember
import ai.rojan.designlab.manager.domain.membership.SalonMemberRole
import ai.rojan.designlab.manager.domain.repository.SalonMembershipRepository

/**
 * Real backend-backed [SalonMembershipRepository] (Phase 2, M4). No
 * cache — unlike [BackendServiceRepository]/[BackendCustomerRepository],
 * there's no consuming screen yet to warrant one; each call talks
 * straight to the backend. `salonId` is a per-call parameter, not a
 * constructor field, matching [SalonMembershipApi]'s own shape — this
 * repository is not scoped to a single salon for its whole lifetime the
 * way the other `Backend*Repository` classes are.
 */
class BackendSalonMembershipRepository(
    private val salonMembershipApi: SalonMembershipApi,
) : SalonMembershipRepository {

    override suspend fun listMembers(salonId: String): Result<List<SalonMember>> =
        safeApiCall { salonMembershipApi.list(salonId) }.map { list -> list.map { it.toDomain() } }

    override suspend fun assignRole(salonId: String, userId: String, role: SalonMemberRole): Result<SalonMember> =
        safeApiCall {
            salonMembershipApi.assign(salonId, userId, AssignMembershipRequestDto(role = role.toNetwork()))
        }.map { it.toDomain() }

    override suspend fun removeMember(salonId: String, userId: String): Result<Unit> =
        safeApiCall { salonMembershipApi.remove(salonId, userId) }

    private fun SalonMembershipResponseDto.toDomain() = SalonMember(
        id = id,
        salonId = salonId,
        userId = userId,
        role = role.toDomain(),
    )

    private fun NetworkSalonRole.toDomain(): SalonMemberRole = when (this) {
        NetworkSalonRole.MANAGER -> SalonMemberRole.MANAGER
        NetworkSalonRole.RECEPTIONIST -> SalonMemberRole.RECEPTIONIST
    }

    private fun SalonMemberRole.toNetwork(): NetworkSalonRole = when (this) {
        SalonMemberRole.MANAGER -> NetworkSalonRole.MANAGER
        SalonMemberRole.RECEPTIONIST -> NetworkSalonRole.RECEPTIONIST
    }
}
