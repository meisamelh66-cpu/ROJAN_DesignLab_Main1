package ai.rojan.designlab.domain.booking

/**
 * The one place [BookingEvent]s are applied to [BookingState] — same
 * role as the sibling [ai.rojan.designlab.domain.customer.EcosystemEventReducer].
 * Every case here is a plain, unconditional `.copy()` — any conditional
 * logic (like "only record intent if not already set") belongs in
 * [BookingEngine], which decides *whether* to emit an event; this class
 * only decides *how* to apply one once it's already been decided.
 */
class BookingEventReducer {

    fun apply(state: BookingState, event: BookingEvent): BookingState = when (event) {
        is BookingEvent.SalonSelected -> state.copy(salonId = event.salonId)
        is BookingEvent.SpecialistSelected -> state.copy(specialistId = event.specialistId)
        is BookingEvent.ServiceSelected -> state.copy(serviceId = event.serviceId)
        is BookingEvent.PackageSelected -> state.copy(packageId = event.packageId)
        is BookingEvent.DateSelected -> state.copy(selectedDateKey = event.dateKey)
        is BookingEvent.TimeSelected -> state.copy(selectedTime = event.time)
        is BookingEvent.PromotionApplied -> state.copy(promotionId = event.promotionId)
        is BookingEvent.CouponApplied -> state.copy(couponId = event.couponId)
        is BookingEvent.IntentDetected -> state.copy(intent = event.intent)
    }

    fun applyAll(state: BookingState, events: List<BookingEvent>): BookingState =
        events.fold(state) { acc, event -> apply(acc, event) }
}
