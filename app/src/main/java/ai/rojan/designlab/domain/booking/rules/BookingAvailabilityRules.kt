package ai.rojan.designlab.domain.booking.rules

import ai.rojan.designlab.data.demo.DemoTimeSlot
import ai.rojan.designlab.data.demo.DemoTimeSlotRepository

/**
 * Availability Rules — the decision layer, per the Booking Availability
 * Architecture Correction. Takes raw slot data (from
 * [ai.rojan.designlab.data.demo.DemoTimeSlotRepository] via
 * [ai.rojan.designlab.domain.booking.BookingEngine]) and decides which
 * are actually bookable. This is the *only* place that decision is
 * made — not [ai.rojan.designlab.data.demo.DemoTimeSlotRepository]
 * (Infrastructure, provides raw data only) and not the UI/ViewModel,
 * which never see this logic at all — they only ever receive the
 * already-decided [DemoTimeSlot] list.
 *
 * Booking Engine completion: [decideAvailability] now takes
 * [durationMinutes] and [specialistId] — a slot is only "available" if
 * (a) it and enough contiguous following slots are free to cover the
 * *entire* service duration without crossing a gap in the schedule
 * (e.g. the 12:00→14:00 lunch break), and (b) availability is decided
 * per-specialist, not per-salon — two specialists at the same salon on
 * the same day genuinely show different open slots.
 */
interface BookingAvailabilityRules {
    fun decideAvailability(
        allSlotTimes: List<String>,
        dateKey: String,
        durationMinutes: Int,
        specialistId: String,
    ): List<DemoTimeSlot>
}

/**
 * TEMPORARY placeholder, pending BOOK 3 import — same caveat as every
 * other `Placeholder*RuleProvider` in this codebase: this is a real
 * algorithm, not fabricated business policy, but it is *demo
 * configuration*, not an approved rule.
 *
 * Booking Engine completion: the base per-slot unavailable set is now
 * seeded by `dateKey + specialistId` together (previously date only) —
 * genuinely different specialists get genuinely different unavailable
 * slots on the same day, not the same pattern relabeled. On top of that
 * base layer, [canFitDuration] does real interval arithmetic: a slot
 * only counts as a valid *start* time if every 30-minute increment
 * needed to cover [durationMinutes] is both time-contiguous (no gap
 * like the lunch break) and itself not in the base-unavailable set.
 */
class PlaceholderBookingAvailabilityRules : BookingAvailabilityRules {

    private companion object {
        const val SLOT_GRANULARITY_MINUTES = 30
    }

    override fun decideAvailability(
        allSlotTimes: List<String>,
        dateKey: String,
        durationMinutes: Int,
        specialistId: String,
    ): List<DemoTimeSlot> {
        // DEMO CONFIGURATION — deterministic per date+specialist, not a
        // real rule. A stable hash picks how many/which base slots are
        // "unavailable" before duration is even considered.
        val seed = "$dateKey|$specialistId".hashCode()
        val unavailableCount = 2 + (Math.abs(seed) % 2) // 2 or 3 slots, deterministic per date+specialist
        val baseUnavailableIndices = allSlotTimes.indices
            .sortedBy { (it * 31 + seed).hashCode() }
            .take(unavailableCount)
            .toSet()

        val startMinutesList = allSlotTimes.map { DemoTimeSlotRepository.toStartMinutes(it) }

        return allSlotTimes.mapIndexed { index, time ->
            val canStartHere = canFitDuration(index, startMinutesList, baseUnavailableIndices, durationMinutes)
            DemoTimeSlot(time = time, startMinutes = startMinutesList[index], available = canStartHere)
        }
    }

    /**
     * Real interval check: can a [durationMinutes]-long booking starting
     * at [startIndex] fit entirely within contiguous, available,
     * 30-minute-apart slots? Walks forward from [startIndex] until the
     * required end time is covered, failing if it ever hits a slot
     * that's unavailable, or a time gap where the next listed slot
     * isn't exactly [SLOT_GRANULARITY_MINUTES] after the current one
     * (e.g. jumping from 12:00 straight to 14:00 over lunch), or simply
     * runs out of slots for the day.
     */
    private fun canFitDuration(
        startIndex: Int,
        startMinutesList: List<Int>,
        unavailableIndices: Set<Int>,
        durationMinutes: Int,
    ): Boolean {
        if (startIndex in unavailableIndices) return false

        val requiredEndMinutes = startMinutesList[startIndex] + durationMinutes
        var currentIndex = startIndex
        var coveredUntil = startMinutesList[startIndex] + SLOT_GRANULARITY_MINUTES

        while (coveredUntil < requiredEndMinutes) {
            val nextIndex = currentIndex + 1
            if (nextIndex >= startMinutesList.size) return false
            if (startMinutesList[nextIndex] != coveredUntil) return false // schedule gap
            if (nextIndex in unavailableIndices) return false

            currentIndex = nextIndex
            coveredUntil += SLOT_GRANULARITY_MINUTES
        }
        return true
    }
}
