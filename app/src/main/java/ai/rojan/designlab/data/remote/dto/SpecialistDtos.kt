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
