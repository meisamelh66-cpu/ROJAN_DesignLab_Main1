package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/** Wire-format role for `ROJAN_Backend/SalonMembershipController` — MANAGER (operational control) or RECEPTIONIST (booking operations only). */
@Serializable
enum class NetworkSalonRole {
    MANAGER,
    RECEPTIONIST,
}

/** Request body for `PUT /api/v1/salons/{salonId}/members/{userId}` (owner only) — the assignee must already have a ROJAN account; there is no invite-by-email flow yet. */
@Serializable
data class AssignMembershipRequestDto(val role: NetworkSalonRole)

@Serializable
data class SalonMembershipResponseDto(
    val id: String,
    val salonId: String,
    val userId: String,
    val role: NetworkSalonRole,
    val createdAt: String,
    val updatedAt: String,
)
