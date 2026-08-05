package ai.rojan.designlab.manager.domain.booking

import ai.rojan.designlab.manager.data.toPersianDigits
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Manager Booking Journey Phase 2 — immutable snapshot of an
 * in-progress manager-created appointment. A new, right-sized type
 * rather than reusing [ai.rojan.designlab.domain.booking.BookingState]
 * (the Customer domain's booking session): that class carries
 * `promotionId`/`couponId`/`paymentMethod`/`intent`, none of which apply
 * here, and has no `customerId` slot at all (the Customer flow never
 * needs one — the app already knows who's logged in), whereas a manager
 * booking on someone's behalf needs exactly that. The *pattern*
 * (immutable state, single source of truth, mutated only through the
 * owning ViewModel) is reused; the concrete fields are not.
 *
 * [time] (Final Release Validation — Real Booking Calendar Integration):
 * holds the exact ISO-8601 local date-time `start` string of a real
 * [ai.rojan.designlab.domain.repository.TimeSlot] returned by the
 * backend's computed-availability API, e.g. `"2026-09-01T10:00:00"` — not
 * a display label. [ManagerBookingViewModel.confirm] sends this value
 * verbatim as `createForCustomer`'s `startTime`, so there is no
 * reformatting/reconstruction step between "what availability said was
 * free" and "what gets booked" that could silently drift.
 * [timeSlotLabel] renders it for display.
 */
data class ManagerBookingState(
    val customerId: String? = null,
    val serviceId: String? = null,
    val specialistId: String? = null,
    val dateKey: String? = null,
    val time: String? = null,
    val isSubmitting: Boolean = false,
    val createdAppointmentId: String? = null,
    val confirmError: String? = null,
) {
    val isReadyToConfirm: Boolean
        get() = customerId != null && serviceId != null && specialistId != null && dateKey != null && time != null
}

/**
 * Fixed daily slot grid (09:00-17:30, 30-minute steps) — used only by
 * [ai.rojan.designlab.manager.data.computeManagerDashboardStats]'s
 * occupancy approximation (a capacity denominator, not a claim about real
 * scheduling). The booking wizard itself no longer uses this: real
 * available time slots now come from the backend's computed-availability
 * API (`AvailabilityController`), not this static grid. Generated rather
 * than hand-typed as Persian-digit string literals, to avoid the exact
 * kind of Unicode transcription mismatch that a hand-typed version of
 * this list previously broke the build with.
 */
val managerBookingTimeSlots: List<String> = (9..17).flatMap { hour ->
    listOf("%02d:00".format(hour), "%02d:30".format(hour))
}.map { it.toPersianDigits() }

/** Persian `HH:mm` display label for a real ISO-8601 local date-time slot [start] value, e.g. `"2026-09-01T10:00:00"` -> `"۱۰:۰۰"`. */
fun timeSlotLabel(start: String): String =
    LocalDateTime.parse(start).format(DateTimeFormatter.ofPattern("HH:mm")).toPersianDigits()
