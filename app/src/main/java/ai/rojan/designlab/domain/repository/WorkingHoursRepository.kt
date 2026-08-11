package ai.rojan.designlab.domain.repository

data class TimeInterval(
    val start: String,
    val end: String,
)

data class SalonWorkingHours(
    val dayOfWeek: String,
    val intervals: List<TimeInterval>,
)

/** Talks to the ROJAN backend's Working Hours API (`ai.rojan.backend.api.schedule.WorkingHoursController`). Read-only from the Customer app's perspective - setting hours is owner-only. */
interface WorkingHoursRepository {
    suspend fun getWorkingHours(salonId: String): Result<List<SalonWorkingHours>>
}
