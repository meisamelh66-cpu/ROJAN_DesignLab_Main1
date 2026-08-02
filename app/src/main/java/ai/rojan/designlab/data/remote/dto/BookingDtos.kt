package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
enum class NetworkBookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED,
}

@Serializable
data class CreateBookingRequestDto(
    val salonId: String,
    val serviceId: String,
    val specialistId: String,
    /** ISO-8601 local date-time, e.g. `"2026-09-01T10:00:00"` — one of the windows returned by the availability endpoint. */
    val startTime: String,
    val notes: String? = null,
)

@Serializable
data class BookingResponseDto(
    val id: String,
    val salonId: String,
    val serviceId: String,
    val specialistId: String,
    val customerId: String,
    val startTime: String,
    val endTime: String,
    val status: NetworkBookingStatus,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String,
)
