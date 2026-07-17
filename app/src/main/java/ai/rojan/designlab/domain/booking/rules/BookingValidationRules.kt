package ai.rojan.designlab.domain.booking.rules

import ai.rojan.designlab.domain.booking.BookingState

/**
 * Booking Validation — one of [ai.rojan.designlab.domain.booking.BookingEngine]'s
 * four currently-implemented rule groups. The one real rule this demo's
 * scope calls for today: confirmation requires service, date, and time
 * all set.
 */
class BookingValidationRules {
    fun isReadyForConfirmation(state: BookingState): Boolean =
        state.serviceId != null && state.selectedDateKey != null && state.selectedTime != null
}
