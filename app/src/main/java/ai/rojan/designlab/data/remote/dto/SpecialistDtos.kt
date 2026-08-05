package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SpecialistResponseDto(
    val id: String,
    val salonId: String,
    val userId: String? = null,
    val displayName: String,
    val bio: String? = null,
    val photoUrl: String? = null,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

/** `CreateSpecialistRequest` (backend, `SpecialistController`) — owner-only. */
@Serializable
data class CreateSpecialistRequestDto(
    val userId: String? = null,
    val displayName: String,
    val bio: String? = null,
    val photoUrl: String? = null,
)

/** `UpdateSpecialistRequest` (backend, `SpecialistController`) — owner-only, full replace (not a PATCH merge like Customer's update). */
@Serializable
data class UpdateSpecialistRequestDto(
    val displayName: String,
    val bio: String? = null,
    val photoUrl: String? = null,
)
