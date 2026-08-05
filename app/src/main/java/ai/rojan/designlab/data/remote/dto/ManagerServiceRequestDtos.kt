package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/** Request body for `POST /api/v1/salons/{salonId}/categories/{categoryId}/services` (owner only). */
@Serializable
data class CreateServiceRequest(
    val name: String,
    val description: String? = null,
    val durationMinutes: Int,
    val price: Double,
)

/** Request body for `PUT /api/v1/salons/{salonId}/categories/{categoryId}/services/{serviceId}` (owner only). */
@Serializable
data class UpdateServiceRequest(
    val name: String,
    val description: String? = null,
    val durationMinutes: Int,
    val price: Double,
)
