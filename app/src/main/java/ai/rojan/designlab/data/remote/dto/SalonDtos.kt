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

/** `CreateSalonRequest` (backend, `SalonController.create`) — owner-authenticated, caller becomes `ownerId`. Field set mirrors the backend request exactly — no `logoUrl`/`latitude`/`longitude`/`city`, none exist on the backend contract yet (Phase A readiness report §1). */
@Serializable
data class CreateSalonRequestDto(
    val name: String,
    val description: String? = null,
    val phone: String,
    val email: String? = null,
    val address: String,
)

/** `UpdateSalonRequest` (backend, `SalonController.update`) — owner-only, full replace (not a PATCH merge). Same field set as [CreateSalonRequestDto]. */
@Serializable
data class UpdateSalonRequestDto(
    val name: String,
    val description: String? = null,
    val phone: String,
    val email: String? = null,
    val address: String,
)
