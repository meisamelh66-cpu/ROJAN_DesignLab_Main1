package ai.rojan.designlab.domain.booking.rules

import ai.rojan.designlab.domain.booking.BookingState

/**
 * Context Completion — one of [ai.rojan.designlab.domain.booking.BookingEngine]'s
 * four currently-implemented rule groups. Renamed from
 * `BookingContextCompletion` to match the `BookingState` naming this
 * migration establishes throughout the domain.
 *
 * Deliberately modest: reconciles [BookingState]'s own internal
 * consistency only, using no external data lookups.
 *
 * Today's one real rule: a selected time with no selected date is a
 * stale/invalid partial state, not a genuine selection — reconciled
 * back to null rather than trusted as-is.
 */
class BookingStateCompletion {
    fun complete(state: BookingState): BookingState =
        if (state.selectedTime != null && state.selectedDateKey == null) {
            state.copy(selectedTime = null)
        } else {
            state
        }
}
