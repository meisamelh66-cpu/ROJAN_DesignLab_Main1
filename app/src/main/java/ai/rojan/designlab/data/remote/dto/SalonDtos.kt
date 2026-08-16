package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/** Wire-format DTO for the ROJAN backend's Salon resource — see `ROJAN_Backend/API_CONTRACT.md`. */
@Serializable
data class SalonResponseDto(
    val id: String,
    val ownerId: String,
    val name: String,
    val description: String? = null,
    val phone: String,
    val email: String? = null,
    val address: String,
    val logoUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String,
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
 * already has, it does not clear them. `logoUrl` is deliberately still
 * not part of this DTO: the backend field exists, but no upload endpoint
 * exists yet to produce a real URL for it, so there is nothing valid an
 * Android screen could write there today.
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
