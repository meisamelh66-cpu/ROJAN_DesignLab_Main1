package ai.rojan.designlab.domain.booking

import ai.rojan.designlab.domain.booking.rules.BookingIntentResolver
import ai.rojan.designlab.domain.booking.rules.BookingStateCompletion
import ai.rojan.designlab.domain.booking.rules.BookingStepResolver
import ai.rojan.designlab.domain.booking.rules.BookingValidationRules

/**
 * Booking orchestration layer — a thin facade delegating to focused rule
 * classes, one per responsibility.
 *
 * Production Data Integrity Phase 1 (Task 7): the demo availability path
 * (`timeSlotsFor`/`hasAnyAvailability`/`earliestAvailableSlot`, backed by
 * `data/demo/DemoTimeSlotRepository` via `BookingAvailabilityRules`) is
 * removed — `BookingTimeScreen`/`RescheduleAppointmentScreen` (its only
 * callers) now read real availability via `AvailabilityRepository`
 * directly (see `BookingTimeViewModel`) or are gated (Reschedule has no
 * backend endpoint). This class keeps only its still-live methods:
 * step/intent resolution, used throughout the real booking flow by
 * [ai.rojan.designlab.presentation.booking.BookingViewModel].
 *
 * Pure Kotlin — no Compose, Android, or Navigation dependency.
 */
class BookingEngine(
    private val stepResolver: BookingStepResolver = BookingStepResolver(),
    private val intentResolver: BookingIntentResolver = BookingIntentResolver(),
    private val validationRules: BookingValidationRules = BookingValidationRules(),
    private val stateCompletion: BookingStateCompletion = BookingStateCompletion(),
) {

    fun determineNextStep(state: BookingState): BookingStep =
        stepResolver.resolve(stateCompletion.complete(state))

    fun decideIntent(state: BookingState, intent: BookingIntent): List<BookingEvent> =
        intentResolver.decide(state, intent)

    fun isReadyForConfirmation(state: BookingState): Boolean =
        validationRules.isReadyForConfirmation(stateCompletion.complete(state))
}
