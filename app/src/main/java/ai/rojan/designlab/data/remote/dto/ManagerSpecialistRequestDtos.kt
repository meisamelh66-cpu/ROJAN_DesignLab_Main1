package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/** Request body for `POST /api/v1/salons/{salonId}/specialists` (owner only). */
@Serializable
data class CreateSpecialistRequestDto(
    val displayName: String,
    val bio: String? = null,
    val photoUrl: String? = null,
    val userId: String? = null,
)

/** Request body for `PUT /api/v1/salons/{salonId}/specialists/{specialistId}` (owner only). */
@Serializable
data class UpdateSpecialistRequestDto(
    val displayName: String,
    val bio: String? = null,
    val photoUrl: String? = null,
)
