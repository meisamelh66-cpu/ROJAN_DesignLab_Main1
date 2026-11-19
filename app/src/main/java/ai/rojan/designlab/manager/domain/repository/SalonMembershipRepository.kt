package ai.rojan.designlab.manager.domain.repository

import ai.rojan.designlab.manager.domain.membership.SalonMember
import ai.rojan.designlab.manager.domain.membership.SalonMemberRole
import ai.rojan.designlab.manager.domain.membership.StaffCandidate

/**
 * Talks to the ROJAN backend's Salon Membership API
 * (`ROJAN_Backend/SalonMembershipController`) — owner-only manager/
 * receptionist role assignment for an existing account.
 *
 * Staff Access Phase 1: the real, approved Phone -> OTP -> User ->
 * SalonMembership -> Role -> Permissions -> Active/Disabled architecture now
 * consumes this (`ManagerStaffAccessScreen`/`ManagerStaffAccessViewModel`) -
 * no longer the dead data-layer-only capability it was in Phase 2, M4.
 */
interface SalonMembershipRepository {

    suspend fun listMembers(salonId: String): Result<List<SalonMember>>

    /** Resolves an existing ROJAN account by phone number, to confirm before assigning it a role. */
    suspend fun lookupByPhone(salonId: String, phone: String): Result<StaffCandidate>

    suspend fun assignRole(salonId: String, userId: String, role: SalonMemberRole): Result<SalonMember>

    /** Disables or re-enables an existing membership in place - never deletes it, never changes [SalonMember.role]. */
    suspend fun setActive(salonId: String, userId: String, active: Boolean): Result<SalonMember>

    /** True removal - distinct from [setActive](salonId, userId, false), which is the normal "disable staff" path. */
    suspend fun removeMember(salonId: String, userId: String): Result<Unit>
}
