package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Request body for `POST /api/v1/salons/{salonId}/invites` - mirrors
 * `ai.rojan.backend.api.salon.CreateSalonInviteRequest` exactly: role only,
 * no phone number (the real invite is token/QR-based, redeemable by
 * whichever authenticated account accepts it).
 */
@Serializable
data class CreateSalonInviteRequestDto(
    val role: String,
)

/**
 * Mirrors `ai.rojan.backend.api.salon.SalonInviteResponse`. [token] is the
 * real, single-use acceptance credential - never logged.
 */
@Serializable
data class SalonInviteResponseDto(
    val id: String,
    val salonId: String,
    val role: String,
    val token: String,
    val status: String,
    val expiresAt: String,
)
