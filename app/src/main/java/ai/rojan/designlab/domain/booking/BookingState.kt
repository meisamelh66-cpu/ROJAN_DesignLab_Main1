package ai.rojan.designlab.domain.booking

/**
 * Immutable snapshot of a booking session's accumulated state — single
 * source of truth for the Booking domain, per the Booking Domain Event
 * Migration. Renamed from `BookingContext` (same fields, same
 * immutability) to match the `State` naming already used by the sibling
 * Customer Ecosystem domain ([ai.rojan.designlab.domain.customer.CustomerEcosystemState]).
 *
 * Plain data class — no Compose, no Android, no Navigation dependency
 * anywhere. Mutated exclusively via [BookingEventReducer] applying a
 * [BookingEvent] — never via a direct `.copy()` call anywhere else,
 * per the corrected architecture this migration establishes.
 */
data class BookingState(
    val intent: BookingIntent = BookingIntent.UNKNOWN,
    val salonId: String? = null,
    val specialistId: String? = null,
    val serviceId: String? = null,
    val packageId: String? = null,
    val selectedDateKey: String? = null,
    val selectedTime: String? = null,
    val promotionId: String? = null,
    val couponId: String? = null,
)
