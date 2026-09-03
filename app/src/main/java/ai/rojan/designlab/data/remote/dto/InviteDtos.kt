package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Wire-format DTOs for `ROJAN_Backend`'s `InviteController`
 * (`GET`/`POST .../accept` under `/api/v1/invites/{token}`) — mirrors
 * `ai.rojan.backend.api.invite.InviteDetailsResponse`/`SalonInviteAcceptedResponse`
 * exactly (confirmed against the real backend source this sprint).
 * [role] reuses [NetworkSalonRole] (`SalonMembershipDtos.kt`) rather than a
 * new enum — same wire vocabulary the backend's `SalonRole` already is for
 * `SalonMembershipResponseDto`.
 */
@Serializable
data class InviteDetailsResponseDto(
    val salonName: String,
    val role: NetworkSalonRole,
)

/** No `membershipId`/`salonName`/`active` — the real backend response carries only these two fields; see [ai.rojan.designlab.reception.domain.repository.SalonInviteAcceptance]'s own doc comment for why the domain contract was corrected to match. */
@Serializable
data class SalonInviteAcceptedResponseDto(
    val salonId: String,
    val role: NetworkSalonRole,
)
