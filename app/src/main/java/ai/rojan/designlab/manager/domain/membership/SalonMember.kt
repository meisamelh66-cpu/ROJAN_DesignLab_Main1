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
)
