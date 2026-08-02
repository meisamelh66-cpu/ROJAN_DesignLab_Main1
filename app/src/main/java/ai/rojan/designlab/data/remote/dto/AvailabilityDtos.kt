package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/** ISO-8601 local date-time strings (no timezone, matches the backend's naive `LocalDateTime` wire format), e.g. `"2026-09-01T10:00:00"`. */
@Serializable
data class TimeSlotResponseDto(
    val start: String,
    val end: String,
)
