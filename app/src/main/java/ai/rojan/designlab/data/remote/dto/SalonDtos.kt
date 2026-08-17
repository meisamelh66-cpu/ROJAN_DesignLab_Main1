package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Wire-format DTO for the ROJAN backend's Salon resource — see
 * `ROJAN_Backend/API_CONTRACT.md`. [logoUrl]/[coverImageUrl] and
 * [logoMediaId]/[coverMediaId] (Central Salon Management — Salon Media UI)
 * are real backend fields, resolved at the response boundary from
 * whichever `MediaAsset` is currently assigned to each identity slot (see
 * `SalonController.toResponse`) — `null` only means the slot is genuinely
 * unset, never "not supported."
 */
@Serializable
data class SalonResponseDto(
    val id: String,
    val ownerId: String,
    val name: String,
    val description: String? = null,
    val phone: String,
    val email: String? = null,
    val address: String,
    val logoMediaId: String? = null,
    val coverMediaId: String? = null,
    val logoUrl: String? = null,
    val coverImageUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

/**
 * `AssignIdentityMediaRequest` (backend, `SalonController.assignIdentityMedia`,
 * `PUT /api/v1/salons/{salonId}/identity-media`) — Central Salon
 * Management — Salon Media UI. [mediaId] `null` clears the slot; a
 * non-null value must reference a `MediaAsset` belonging to this salon
 * whose `mediaType` matches [slot] (enforced backend-side, not here).
 */
@Serializable
data class AssignIdentityMediaRequestDto(
    val slot: String,
    val mediaId: String? = null,
)

/** `CreateSalonRequest` (backend, `SalonController.create`) — owner-authenticated, caller becomes `ownerId`. Field set mirrors the backend request exactly. `CreateSalonRequest` itself has no `logoUrl`/`latitude`/`longitude`/`city` fields (verified directly against `ROJAN_Backend/api/.../salon/SalonDtos.kt`) — a new salon's coordinates are set afterward via [UpdateSalonRequestDto], not at creation time. */
@Serializable
data class CreateSalonRequestDto(
    val name: String,
    val description: String? = null,
    val phone: String,
    val email: String? = null,
    val address: String,
)

/**
 * `UpdateSalonRequest` (backend, `SalonController.update`) — owner-only,
 * full replace for `name`/`description`/`phone`/`email`/`address`.
 * [latitude]/[longitude] are real, live backend fields
 * (`Salon.updateProfile()`, verified directly against `ROJAN_Backend`
 * source) with "null means leave unchanged" merge semantics — omitting
 * them (or sending `null`) preserves whatever coordinates the salon
 * already has, it does not clear them. `logoUrl`/`coverImageUrl` are
 * still deliberately not part of this DTO — identity media is written
 * through the dedicated `PUT /identity-media` endpoint
 * ([AssignIdentityMediaRequestDto]), never through this salon-fields
 * replace, matching the backend's own separation of the two concerns.
 */
@Serializable
data class UpdateSalonRequestDto(
    val name: String,
    val description: String? = null,
    val phone: String,
    val email: String? = null,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
)
