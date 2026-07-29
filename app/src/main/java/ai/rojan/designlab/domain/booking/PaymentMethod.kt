package ai.rojan.designlab.domain.booking

/**
 * How a customer chose to pay for a booking — pure business/domain
 * concept, no Compose/Navigation/display-label dependency (mirrors
 * [BookingIntent]'s shape; display labels stay in the UI layer).
 *
 * Customer Journey Audit Phase A (P0-2): previously this choice existed
 * only as local UI state on [ai.rojan.designlab.screens.bookingflow.BookingConfirmationScreen]
 * and was never recorded anywhere — selecting either option had zero
 * effect on the booking. Promoted to a real [BookingState] field so the
 * selection is actually recorded.
 */
enum class PaymentMethod {
    WALLET,
    PAY_AT_SALON,
}
