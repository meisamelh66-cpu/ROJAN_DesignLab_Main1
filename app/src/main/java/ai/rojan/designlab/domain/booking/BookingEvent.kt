package ai.rojan.designlab.domain.booking

/**
 * Every Booking domain state mutation, as an event — same shape as the
 * sibling [ai.rojan.designlab.domain.customer.EcosystemEvent]. This is
 * the *only* channel [BookingState] changes through;
 * [ai.rojan.designlab.presentation.booking.BookingViewModel] never
 * mutates state directly, only dispatches these to
 * [BookingEventReducer].
 */
sealed interface BookingEvent {
    data class SalonSelected(val salonId: String) : BookingEvent
    data class SpecialistSelected(val specialistId: String) : BookingEvent
    data class ServiceSelected(val serviceId: String) : BookingEvent
    data class PackageSelected(val packageId: String) : BookingEvent
    data class DateSelected(val dateKey: String) : BookingEvent
    data class TimeSelected(val time: String) : BookingEvent
    data class PromotionApplied(val promotionId: String) : BookingEvent
    data class CouponApplied(val couponId: String) : BookingEvent

    /**
     * Unconditional by the time it reaches the Reducer — [BookingEngine.decideIntent]
     * is where the "only if still UNKNOWN" business logic lives (intent
     * resolution is an Engine responsibility per this migration's own
     * task list); this event, once emitted, is always applied.
     */
    data class IntentDetected(val intent: BookingIntent) : BookingEvent
}
