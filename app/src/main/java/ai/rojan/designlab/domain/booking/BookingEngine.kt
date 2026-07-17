package ai.rojan.designlab.domain.booking

import ai.rojan.designlab.data.demo.DemoTimeSlot
import ai.rojan.designlab.data.demo.DemoTimeSlotRepository
import ai.rojan.designlab.domain.booking.rules.BookingAvailabilityRules
import ai.rojan.designlab.domain.booking.rules.BookingIntentResolver
import ai.rojan.designlab.domain.booking.rules.BookingStateCompletion
import ai.rojan.designlab.domain.booking.rules.BookingStepResolver
import ai.rojan.designlab.domain.booking.rules.BookingValidationRules
import ai.rojan.designlab.domain.booking.rules.PlaceholderBookingAvailabilityRules

/**
 * Booking orchestration layer — same shape as the sibling
 * [ai.rojan.designlab.domain.customer.CustomerEcosystemEngine]: a thin
 * facade delegating to focused rule classes, one per responsibility.
 *
 * **Booking Availability Architecture Correction addition:** [timeSlotsFor]
 * is the one place this class touches Infrastructure directly — per the
 * required "Infrastructure → BookingAvailabilityRules → BookingEngine →
 * UI" chain, it reads raw data from
 * [ai.rojan.designlab.data.demo.DemoTimeSlotRepository] and immediately
 * hands it to [BookingAvailabilityRules] for the actual availability
 * decision, never deciding availability itself. UI calls this method,
 * never the repository or the rule provider directly.
 *
 * Pure Kotlin — no Compose, Android, or Navigation dependency.
 */
class BookingEngine(
    private val stepResolver: BookingStepResolver = BookingStepResolver(),
    private val intentResolver: BookingIntentResolver = BookingIntentResolver(),
    private val validationRules: BookingValidationRules = BookingValidationRules(),
    private val stateCompletion: BookingStateCompletion = BookingStateCompletion(),
    private val availabilityRules: BookingAvailabilityRules = PlaceholderBookingAvailabilityRules(),
) {

    fun determineNextStep(state: BookingState): BookingStep =
        stepResolver.resolve(stateCompletion.complete(state))

    fun decideIntent(state: BookingState, intent: BookingIntent): List<BookingEvent> =
        intentResolver.decide(state, intent)

    fun isReadyForConfirmation(state: BookingState): Boolean =
        validationRules.isReadyForConfirmation(stateCompletion.complete(state))

    /** Infrastructure → BookingAvailabilityRules → here → UI. */
    fun timeSlotsFor(date: String, durationMinutes: Int, specialistId: String): List<DemoTimeSlot> =
        availabilityRules.decideAvailability(DemoTimeSlotRepository.allSlotTimes, date, durationMinutes, specialistId)

    /** Does this date have at least one valid slot for this specialist/duration? Used for auto-preselecting the first genuinely available day. */
    fun hasAnyAvailability(date: String, durationMinutes: Int, specialistId: String): Boolean =
        timeSlotsFor(date, durationMinutes, specialistId).any { it.available }

    /** Earliest bookable slot (by start time) for this specialist on this date, or null if none - the basis for "earliest available specialist" ordering. */
    fun earliestAvailableSlot(date: String, durationMinutes: Int, specialistId: String): DemoTimeSlot? =
        timeSlotsFor(date, durationMinutes, specialistId)
            .filter { it.available }
            .minByOrNull { it.startMinutes }
}
