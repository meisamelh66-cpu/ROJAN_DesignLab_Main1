package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Deliberately separate from the authenticated Salon/Specialist DTOs —
 * mirrors `ROJAN_Backend`'s own `PublicSalonResponse` family, which
 * carries no `ownerId`/`userId` linkage fields since any unauthenticated
 * caller can request these.
 */
@Serializable
data class PublicSalonResponseDto(
    val id: String,
    val name: String,
    val description: String?,
    val phone: String,
    val address: String,
    val logoUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
)

@Serializable
data class PublicServiceCategoryResponseDto(
    val id: String,
    val name: String,
    val description: String?,
)

@Serializable
data class PublicServiceResponseDto(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String?,
    val durationMinutes: Int,
    val price: Double,
)

@Serializable
data class PublicSpecialistResponseDto(
    val id: String,
    val displayName: String,
    val bio: String?,
    val photoUrl: String?,
)
