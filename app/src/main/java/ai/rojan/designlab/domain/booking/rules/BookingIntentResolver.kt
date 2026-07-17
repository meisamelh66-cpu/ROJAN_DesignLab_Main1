package ai.rojan.designlab.domain.booking.rules

import ai.rojan.designlab.domain.booking.BookingEvent
import ai.rojan.designlab.domain.booking.BookingIntent
import ai.rojan.designlab.domain.booking.BookingState

/**
 * Intent Resolution — one of [ai.rojan.designlab.domain.booking.BookingEngine]'s
 * four currently-implemented rule groups. A session's intent reflects
 * how it *started*; once recorded, a later action shouldn't overwrite
 * that origin.
 *
 * **Booking Domain Event Migration correction:** previously this method
 * returned a new [BookingState] directly — a state mutation happening
 * outside [ai.rojan.designlab.domain.booking.BookingEventReducer], the
 * exact inconsistency this migration removes. Now it returns
 * `List<BookingEvent>` instead: the conditional "only if not already
 * set" logic (a real decision) still lives here, but *applying* that
 * decision is the Reducer's job, not this class's.
 */
class BookingIntentResolver {
    fun decide(state: BookingState, intent: BookingIntent): List<BookingEvent> =
        if (state.intent == BookingIntent.UNKNOWN) listOf(BookingEvent.IntentDetected(intent)) else emptyList()
}
