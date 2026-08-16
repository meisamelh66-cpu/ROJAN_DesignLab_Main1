package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/** Wire-format DTOs for the ROJAN backend's Working Hours API (`ai.rojan.backend.api.schedule.WorkingHoursDtos`). Times/day-of-week are read as opaque strings (e.g. "09:00:00", "MONDAY"), same as every other date/time field elsewhere in this app's DTOs - no date/time parsing library exists in this codebase. */

@Serializable
data class TimeIntervalResponseDto(
    val start: String,
    val end: String,
)

@Serializable
data class WorkingHoursResponseDto(
    val id: String,
    val salonId: String,
    val dayOfWeek: String,
    val intervals: List<TimeIntervalResponseDto>,
)

/** Request-side mirror of [TimeIntervalResponseDto] — same opaque-string treatment, matching backend's `TimeIntervalDto` (`WorkingHoursDtos.kt`, `ROJAN_Backend`). */
@Serializable
data class TimeIntervalRequestDto(
    val start: String,
    val end: String,
)

/** Wire body for `PUT /api/v1/salons/{salonId}/working-hours/{dayOfWeek}` (owner-only), matching backend's `SetWorkingHoursRequest` field-for-field. */
@Serializable
data class SetWorkingHoursRequestDto(
    val intervals: List<TimeIntervalRequestDto>,
)
