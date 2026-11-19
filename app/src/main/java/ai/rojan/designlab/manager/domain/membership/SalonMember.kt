package ai.rojan.designlab.manager.domain.membership

/** Mirrors the backend's `SalonRole` exactly — MANAGER (operational control) or RECEPTIONIST (booking operations only). */
enum class SalonMemberRole {
    MANAGER,
    RECEPTIONIST,
}

/** A staff member's role assignment at a salon — never a Customer, and never the owner (ownership isn't a membership row). */
data class SalonMember(
    val id: String,
    val salonId: String,
    val userId: String,
    val role: SalonMemberRole,
    /** Staff Access Phase 1: disabled keeps the row/role but grants no permissions — see the backend's `SalonPermissionResolver`. */
    val active: Boolean,
    val fullName: String,
    val phoneNumber: String?,
)

/** Staff Access Phase 1: the minimum identity fields returned by a staff-candidate phone lookup, before a role is assigned. */
data class StaffCandidate(
    val userId: String,
    val fullName: String,
    val phoneNumber: String,
)
