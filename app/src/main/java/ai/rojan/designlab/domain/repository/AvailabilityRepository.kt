package ai.rojan.designlab.domain.repository

/** A bookable window, in local (no-timezone) ISO-8601 date-time form, e.g. `"2026-09-01T10:00:00"`. */
data class TimeSlot(
    val start: String,
    val end: String,
)

/** Talks to the ROJAN backend's computed-availability API (`ROJAN_Backend/API_CONTRACT.md`). */
interface AvailabilityRepository {
    suspend fun getAvailableSlots(
        salonId: String,
        specialistId: String,
        serviceId: String,
        date: String,
        slotIntervalMinutes: Int = 15,
    ): Result<List<TimeSlot>>
}
