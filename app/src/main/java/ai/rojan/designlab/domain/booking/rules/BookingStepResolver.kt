package ai.rojan.designlab.domain.booking.rules

import ai.rojan.designlab.domain.booking.BookingState
import ai.rojan.designlab.domain.booking.BookingStep

/**
 * Next-Step Resolution — one of [ai.rojan.designlab.domain.booking.BookingEngine]'s
 * four currently-implemented rule groups. No fixed sequence: only asks
 * for what's still missing. Salon/Specialist are optional enrichment,
 * never a hard gate; Service is the one required gate.
 */
class BookingStepResolver {
    fun resolve(state: BookingState): BookingStep = when {
        state.serviceId == null -> BookingStep.SEARCH
        state.selectedDateKey == null -> BookingStep.DATE
        state.selectedTime == null -> BookingStep.TIME
        else -> BookingStep.CONFIRMATION
    }
}
