package ai.rojan.designlab.presentation.booking

import ai.rojan.designlab.domain.booking.BookingEngine
import ai.rojan.designlab.domain.booking.BookingEvent
import ai.rojan.designlab.domain.booking.BookingEventReducer
import ai.rojan.designlab.domain.booking.BookingIntent
import ai.rojan.designlab.domain.booking.BookingState
import ai.rojan.designlab.domain.booking.PaymentMethod
import ai.rojan.designlab.domain.booking.BookingStep
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

/**
 * Session-scoped booking UI state.
 *
 * **Booking Domain Event Migration:** this class previously mutated
 * [BookingState] (then named `BookingContext`) directly via `.copy()` —
 * a real architectural inconsistency with the sibling
 * [ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel],
 * which was already event-driven. That inconsistency is what this
 * migration removes. Every method now follows the exact same two-step
 * shape as its sibling: ask [BookingEngine] to *decide* (for the one
 * method with real conditional logic, [onIntentDetected]) or construct
 * the event directly (for the simple, unconditional "record this
 * selection" methods — no Engine round-trip needed when there's no
 * decision to make, only a value to record), then ask
 * [BookingEventReducer] to *apply* it. This class itself makes no
 * business decisions and performs no state mutation logic of its own —
 * per "BookingViewModel must only: receive intents/events, dispatch
 * events, expose state."
 *
 * Ownership/lifecycle unchanged: obtained scoped to the booking flow's
 * nested Navigation graph back-stack entry (see `RojanNavGraph.kt`) —
 * that's what makes "survives navigation within the flow, fresh
 * instance for the next session" true by construction.
 *
 * **Process-death fix (Critical UX bug — reproduced, fixed, and
 * re-confirmed via a real `am kill` device test on 2026-07-28):** the
 * nested graph's own back-stack entry survives process death (Jetpack
 * Navigation restores routes automatically), but this class's [state]
 * previously lived only in plain `mutableStateOf` — so a real customer
 * who switched away mid-booking to read their OTP SMS (a very ordinary
 * thing to do, and exactly the kind of pause Android reclaims
 * backgrounded memory during) would return to find the "resume booking"
 * branch correctly detected as in-progress, but resuming into a
 * brand-new, empty [BookingState] — functionally identical to the flow
 * restarting from scratch. An earlier attempted fix (a legacy
 * `AbstractSavedStateViewModelFactory(owner, defaultArgs)` construction —
 * see [BookingViewModelFactory]'s own history) did not actually restore
 * correctly; the real fix was migrating to `createSavedStateHandle()`
 * sourced from `parentEntry.defaultViewModelCreationExtras` at the
 * `bookingViewModelFor` call site in `RojanNavGraph.kt` — the API
 * Navigation-Compose is actually built to restore through. [savedStateHandle]
 * persists every field on each [dispatch] and restores them in
 * [restoreState]; verified end-to-end (real process kill, not just
 * backgrounding) that salonId/serviceId/selectedDateKey/selectedTime
 * survive and [restoreState] rebuilds the exact pre-kill [BookingState].
 */
class BookingViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val engine: BookingEngine = BookingEngine(),
    private val reducer: BookingEventReducer = BookingEventReducer(),
) : ViewModel() {

    var state by mutableStateOf(restoreState())
        private set

    private fun restoreState(): BookingState {
        return BookingState(
            intent = savedStateHandle.get<String>(KEY_INTENT)
                ?.let { runCatching { BookingIntent.valueOf(it) }.getOrNull() } ?: BookingIntent.UNKNOWN,
            salonId = savedStateHandle[KEY_SALON_ID],
            specialistId = savedStateHandle[KEY_SPECIALIST_ID],
            serviceId = savedStateHandle[KEY_SERVICE_ID],
            packageId = savedStateHandle[KEY_PACKAGE_ID],
            selectedDateKey = savedStateHandle[KEY_DATE_KEY],
            selectedTime = savedStateHandle[KEY_TIME],
            promotionId = savedStateHandle[KEY_PROMOTION_ID],
            couponId = savedStateHandle[KEY_COUPON_ID],
            paymentMethod = savedStateHandle.get<String>(KEY_PAYMENT_METHOD)
                ?.let { runCatching { PaymentMethod.valueOf(it) }.getOrNull() } ?: PaymentMethod.WALLET,
        )
    }

    private fun persistState(newState: BookingState) {
        savedStateHandle[KEY_INTENT] = newState.intent.name
        savedStateHandle[KEY_SALON_ID] = newState.salonId
        savedStateHandle[KEY_SPECIALIST_ID] = newState.specialistId
        savedStateHandle[KEY_SERVICE_ID] = newState.serviceId
        savedStateHandle[KEY_PACKAGE_ID] = newState.packageId
        savedStateHandle[KEY_DATE_KEY] = newState.selectedDateKey
        savedStateHandle[KEY_TIME] = newState.selectedTime
        savedStateHandle[KEY_PROMOTION_ID] = newState.promotionId
        savedStateHandle[KEY_COUPON_ID] = newState.couponId
        savedStateHandle[KEY_PAYMENT_METHOD] = newState.paymentMethod.name
    }

    private fun dispatch(events: List<BookingEvent>) {
        state = reducer.applyAll(state, events)
        persistState(state)
    }

    fun onSalonSelected(id: String) = dispatch(listOf(BookingEvent.SalonSelected(id)))
    fun onSpecialistSelected(id: String) = dispatch(listOf(BookingEvent.SpecialistSelected(id)))
    fun onServiceSelected(id: String) = dispatch(listOf(BookingEvent.ServiceSelected(id)))
    fun onPackageSelected(id: String) = dispatch(listOf(BookingEvent.PackageSelected(id)))
    fun onDateSelected(dateKey: String) = dispatch(listOf(BookingEvent.DateSelected(dateKey)))
    fun onTimeSelected(time: String) = dispatch(listOf(BookingEvent.TimeSelected(time)))
    fun onPromotionApplied(id: String) = dispatch(listOf(BookingEvent.PromotionApplied(id)))
    fun onCouponApplied(id: String) = dispatch(listOf(BookingEvent.CouponApplied(id)))
    fun onPaymentMethodSelected(method: PaymentMethod) = dispatch(listOf(BookingEvent.PaymentMethodSelected(method)))

    /** The one method with real conditional logic — delegated to the Engine, per "intent resolution" being its responsibility. */
    fun onIntentDetected(intent: BookingIntent) = dispatch(engine.decideIntent(state, intent))

    /** Delegates entirely to [BookingEngine] — this class makes no decision of its own here. */
    fun nextStep(): BookingStep = engine.determineNextStep(state)

    fun isReadyForConfirmation(): Boolean = engine.isReadyForConfirmation(state)

    private companion object {
        const val KEY_INTENT = "booking_intent"
        const val KEY_SALON_ID = "booking_salon_id"
        const val KEY_SPECIALIST_ID = "booking_specialist_id"
        const val KEY_SERVICE_ID = "booking_service_id"
        const val KEY_PACKAGE_ID = "booking_package_id"
        const val KEY_DATE_KEY = "booking_date_key"
        const val KEY_TIME = "booking_time"
        const val KEY_PROMOTION_ID = "booking_promotion_id"
        const val KEY_COUPON_ID = "booking_coupon_id"
        const val KEY_PAYMENT_METHOD = "booking_payment_method"
    }
}
