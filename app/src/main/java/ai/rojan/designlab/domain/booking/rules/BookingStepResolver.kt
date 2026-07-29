package ai.rojan.designlab.domain.booking.rules

import ai.rojan.designlab.domain.booking.BookingState
import ai.rojan.designlab.domain.booking.BookingStep

/**
 * Next-Step Resolution — one of [ai.rojan.designlab.domain.booking.BookingEngine]'s
 * four currently-implemented rule groups. No fixed sequence: only asks
 * for what's still missing. Salon is optional enrichment, never a hard
 * gate; Service is one required gate.
 *
 * Customer Journey Audit Phase A (P0-1) fix: Specialist is now a
 * conditional gate too — asked whenever [BookingState.specialistId] is
 * still null at this point. This relies on the call site (see
 * [ai.rojan.designlab.navigation.RojanNavGraph]'s `SERVICE_DETAILS`
 * composable) having already auto-selected the specialist beforehand
 * when a salon genuinely has zero or one specialist — by the time
 * `resolve` runs, a null `specialistId` means there's a real choice to
 * make, not just an unset default.
 */
class BookingStepResolver {
    fun resolve(state: BookingState): BookingStep = when {
        state.serviceId == null -> BookingStep.SEARCH
        state.specialistId == null -> BookingStep.SPECIALIST
        state.selectedDateKey == null -> BookingStep.DATE
        state.selectedTime == null -> BookingStep.TIME
        else -> BookingStep.CONFIRMATION
    }
}
