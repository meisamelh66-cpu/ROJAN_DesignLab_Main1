package ai.rojan.designlab.domain.booking

import kotlinx.serialization.Serializable

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
 *
 * P0-7: `@Serializable` so this same enum can be embedded directly in
 * [ai.rojan.designlab.data.remote.dto.CreateBookingRequestDto] and sent to
 * the backend — names must stay identical to the backend's
 * `ai.rojan.backend.domain.booking.PaymentMethod` (Jackson deserializes by
 * enum name).
 */
@Serializable
enum class PaymentMethod {
    WALLET,
    PAY_AT_SALON,
}
