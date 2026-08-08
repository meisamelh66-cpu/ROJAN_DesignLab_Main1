package ai.rojan.designlab.manager.domain.repository

import ai.rojan.designlab.manager.domain.membership.SalonMember
import ai.rojan.designlab.manager.domain.membership.SalonMemberRole

/**
 * Talks to the ROJAN backend's Salon Membership API
 * (`ROJAN_Backend/SalonMembershipController`) — owner-only manager/
 * receptionist role assignment for an existing account.
 *
 * Data layer only this phase (Phase 2, M4): no screen consumes this yet
 * — Staff/member-management/invite/role-assignment UI is a separate,
 * later decision (same carve-out as [ai.rojan.designlab.domain.repository.PublicSalonRepository],
 * Phase 2 C3).
 */
interface SalonMembershipRepository {

    suspend fun listMembers(salonId: String): Result<List<SalonMember>>

    suspend fun assignRole(salonId: String, userId: String, role: SalonMemberRole): Result<SalonMember>

    suspend fun removeMember(salonId: String, userId: String): Result<Unit>
}
