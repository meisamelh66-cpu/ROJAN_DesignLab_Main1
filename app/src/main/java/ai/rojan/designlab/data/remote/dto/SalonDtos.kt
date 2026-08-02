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
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String,
)
