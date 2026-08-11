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
