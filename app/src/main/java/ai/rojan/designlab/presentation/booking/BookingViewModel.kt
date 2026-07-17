package ai.rojan.designlab.presentation.booking

import ai.rojan.designlab.domain.booking.BookingEngine
import ai.rojan.designlab.domain.booking.BookingEvent
import ai.rojan.designlab.domain.booking.BookingEventReducer
import ai.rojan.designlab.domain.booking.BookingIntent
import ai.rojan.designlab.domain.booking.BookingState
import ai.rojan.designlab.domain.booking.BookingStep
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
 */
class BookingViewModel(
    private val engine: BookingEngine = BookingEngine(),
    private val reducer: BookingEventReducer = BookingEventReducer(),
) : ViewModel() {

    var state by mutableStateOf(BookingState())
        private set

    private fun dispatch(events: List<BookingEvent>) {
        state = reducer.applyAll(state, events)
    }

    fun onSalonSelected(id: String) = dispatch(listOf(BookingEvent.SalonSelected(id)))
    fun onSpecialistSelected(id: String) = dispatch(listOf(BookingEvent.SpecialistSelected(id)))
    fun onServiceSelected(id: String) = dispatch(listOf(BookingEvent.ServiceSelected(id)))
    fun onPackageSelected(id: String) = dispatch(listOf(BookingEvent.PackageSelected(id)))
    fun onDateSelected(dateKey: String) = dispatch(listOf(BookingEvent.DateSelected(dateKey)))
    fun onTimeSelected(time: String) = dispatch(listOf(BookingEvent.TimeSelected(time)))
    fun onPromotionApplied(id: String) = dispatch(listOf(BookingEvent.PromotionApplied(id)))
    fun onCouponApplied(id: String) = dispatch(listOf(BookingEvent.CouponApplied(id)))

    /** The one method with real conditional logic — delegated to the Engine, per "intent resolution" being its responsibility. */
    fun onIntentDetected(intent: BookingIntent) = dispatch(engine.decideIntent(state, intent))

    /** Delegates entirely to [BookingEngine] — this class makes no decision of its own here. */
    fun nextStep(): BookingStep = engine.determineNextStep(state)

    fun isReadyForConfirmation(): Boolean = engine.isReadyForConfirmation(state)
}
