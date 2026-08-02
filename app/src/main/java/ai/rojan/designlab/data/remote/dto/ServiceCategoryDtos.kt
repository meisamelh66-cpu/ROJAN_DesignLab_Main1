package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ServiceCategoryResponseDto(
    val id: String,
    val salonId: String,
    val name: String,
    val description: String? = null,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String,
)
