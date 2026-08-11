package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/** Wire-format DTOs for the ROJAN backend's Customer Relationship API (`ROJAN_Backend` commit db5faea, `SalonFollowResponse`/`SalonFavoriteResponse`). Reused for both the POST mutation response and as list items under `PagedResponseDto` - the backend returns an identical shape for both, so a separate "list item" DTO would just be a byte-for-byte duplicate. */

@Serializable
data class SalonFollowDto(
    val id: String,
    val salonId: String,
    val status: String,
    val createdAt: String,
)

@Serializable
data class SalonFavoriteDto(
    val id: String,
    val salonId: String,
    val createdAt: String,
)
