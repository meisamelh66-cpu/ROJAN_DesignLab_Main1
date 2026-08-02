package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ServiceResponseDto(
    val id: String,
    val salonId: String,
    val categoryId: String,
    val name: String,
    val description: String? = null,
    val durationMinutes: Int,
    /** Backend represents this as a `BigDecimal`; a JSON number decodes cleanly into [Double]. */
    val price: Double,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String,
)
