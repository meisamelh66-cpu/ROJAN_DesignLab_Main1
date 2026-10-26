package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Wire-format DTOs for `ROJAN_Backend`'s `SalonInviteController`
 * (`POST`/`GET`/`DELETE .../salons/{salonId}/invites`) — mirrors
 * `ai.rojan.backend.api.salon.SalonInviteDtos.kt`/`CreateSalonInviteRequest`/
 * `SalonInviteResponse` exactly (confirmed against the real backend source
 * this sprint, P1-6). [CreateSalonInviteRequestDto] deliberately carries
 * only [role] — the real endpoint has no phone/email/TTL field; an invite
 * is anonymous and QR/link-redeemable, not targeted at a known contact.
 */
@Serializable
data class CreateSalonInviteRequestDto(
    val role: NetworkSalonRole,
)

/**
 * [status] mirrors the backend's `SalonInviteStatus` exactly. `EXPIRED` is
 * never a stored value on the backend row — it's computed at read time from
 * [expiresAt] (`SalonInvite.currentStatus()`) — so this field can report a
 * different value across two consecutive fetches of the same invite with no
 * mutation in between; callers must not cache/diff it client-side.
 */
@Serializable
enum class NetworkSalonInviteStatus {
    CREATED,
    ACCEPTED,
    EXPIRED,
    REVOKED,
}

@Serializable
data class SalonInviteResponseDto(
    val id: String,
    val salonId: String,
    val role: NetworkSalonRole,
    val token: String,
    val status: NetworkSalonInviteStatus,
    val expiresAt: String,
    val createdBy: String,
    val acceptedBy: String? = null,
    val createdAt: String,
    val updatedAt: String,
)
