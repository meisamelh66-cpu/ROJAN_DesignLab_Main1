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
    /** Manager Booking Creation Integrity follow-up: set only when a salon owner is booking on behalf of a customer returned by `GET /salons/{salonId}/customers`. Omitted for the normal self-booking case. */
    val customerId: String? = null,
)

/** TEAM2-003: request body for `PUT /api/v1/bookings/{id}/reschedule` — mirrors the backend's `RescheduleBookingRequest`. */
@Serializable
data class RescheduleBookingRequestDto(
    /** ISO-8601 local date-time, same format/source as [CreateBookingRequestDto.startTime]. */
    val newStartTime: String,
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
