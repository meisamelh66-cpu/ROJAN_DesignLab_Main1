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

/** Staff Access Phase 1: request body for `PATCH /api/v1/salons/{salonId}/members/{userId}/status` — disable/re-enable only, never touches `role`. */
@Serializable
data class SetMembershipActiveRequestDto(val active: Boolean)

@Serializable
data class SalonMembershipResponseDto(
    val id: String,
    val salonId: String,
    val userId: String,
    val role: NetworkSalonRole,
    val active: Boolean,
    val fullName: String,
    val phoneNumber: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

/** Staff Access Phase 1: response for `GET /api/v1/salons/{salonId}/members/lookup?phone=...` — the minimum identity fields to confirm a match before assigning. */
@Serializable
data class StaffLookupResponseDto(
    val userId: String,
    val fullName: String,
    val phoneNumber: String,
)
