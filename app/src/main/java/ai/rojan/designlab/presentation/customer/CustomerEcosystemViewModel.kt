package ai.rojan.designlab.presentation.customer

import ai.rojan.designlab.data.demo.DemoCoupon
import ai.rojan.designlab.data.demo.DemoUserReview
import ai.rojan.designlab.domain.customer.CustomerEcosystemEngine
import ai.rojan.designlab.domain.customer.CustomerEcosystemState
import ai.rojan.designlab.domain.customer.EcosystemEvent
import ai.rojan.designlab.domain.customer.EcosystemEventReducer
import ai.rojan.designlab.domain.reminder.DemoReminderPreference
import ai.rojan.designlab.domain.reminder.InMemoryReminderRepository
import ai.rojan.designlab.domain.reminder.NoOpReminderScheduler
import ai.rojan.designlab.domain.reminder.ReminderRepository
import ai.rojan.designlab.domain.reminder.ReminderScheduler
import ai.rojan.designlab.domain.reminder.ReminderStatus
import ai.rojan.designlab.domain.reminder.ReminderTime
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * Session-scoped, shared customer ecosystem state.
 *
 * **Architecture correction from the previous revision:** every action
 * method now follows the same two-step shape — ask [CustomerEcosystemEngine]
 * to *compute* what happened (`List<EcosystemEvent>`), then ask
 * [EcosystemEventReducer] to *apply* those events to [state]. This
 * class itself makes no business decisions and performs no state
 * mutation logic of its own — it only holds state, receives UI events,
 * and wires the two domain classes together, per "every state
 * transition must remain isolated inside Engine classes."
 */
class CustomerEcosystemViewModel(
    private val engine: CustomerEcosystemEngine = CustomerEcosystemEngine(),
    private val reducer: EcosystemEventReducer = EcosystemEventReducer(),
) : ViewModel() {

    var state by mutableStateOf(CustomerEcosystemState())
        private set

    var lastEvents by mutableStateOf<List<EcosystemEvent>>(emptyList())
        private set

    private fun dispatch(events: List<EcosystemEvent>) {
        state = reducer.applyAll(state, events)
        lastEvents = events
    }

    fun completeAppointment(appointmentId: String) {
        dispatch(engine.completeAppointment(state, appointmentId))
    }

    /** Appointment System completion (V1.0 Module 6 - Cancel). */
    fun cancelAppointment(appointmentId: String) {
        dispatch(engine.cancelAppointment(state, appointmentId))
    }

    /** Appointment System completion (V1.0 Module 6 - Reschedule). */
    fun rescheduleAppointment(appointmentId: String, newDateKey: String, newDateLabel: String, newTime: String) {
        dispatch(engine.rescheduleAppointment(state, appointmentId, newDateKey, newDateLabel, newTime))
    }

    // ── Appointment System completion (V1.0 Module 6 - Waiting List) ──

    fun joinWaitlist(
        salonId: String,
        salonName: String,
        serviceId: String,
        serviceName: String,
        specialistId: String?,
        dateKey: String,
        dateLabel: String,
    ) {
        dispatch(engine.joinWaitlist(state, salonId, salonName, serviceId, serviceName, specialistId, dateKey, dateLabel))
    }

    fun leaveWaitlist(entryId: String) {
        dispatch(engine.leaveWaitlist(state, entryId))
    }

    // ── Appointment System completion (V1.0 Module 6 - Reminder) ──

    private val reminderRepository: ReminderRepository =
        InMemoryReminderRepository()
    private val reminderScheduler: ReminderScheduler =
        NoOpReminderScheduler()

    fun setReminderPreference(
        appointmentId: String,
        enabled: Boolean,
        reminderTime: ReminderTime,
        appointmentDateLabel: String,
        appointmentTime: String,
    ) {
        val preference = DemoReminderPreference(
            appointmentId = appointmentId,
            enabled = enabled,
            reminderTime = reminderTime,
            status = if (enabled) ReminderStatus.SCHEDULED else ReminderStatus.CANCELLED,
        )
        reminderRepository.savePreference(preference)
        if (enabled) {
            reminderScheduler.schedule(preference, appointmentDateLabel, appointmentTime)
        } else {
            reminderScheduler.cancel(appointmentId)
        }
        state = state.copy(reminderPreferences = reminderRepository.getAllPreferences())
    }

    fun redeemCoupon(coupon: DemoCoupon, referencePrice: Int) {
        dispatch(engine.redeemCoupon(state, coupon, referencePrice))
    }

    fun requestReview(appointmentId: String) {
        dispatch(engine.requestReview(state, appointmentId))
    }

    fun submitReview(appointmentId: String, review: DemoUserReview) {
        dispatch(engine.submitReview(state, appointmentId, review))
    }

    fun publishReview(appointmentId: String) {
        dispatch(engine.publishReview(state, appointmentId))
    }

    fun toggleFavoriteSalon(salonId: String) {
        dispatch(engine.toggleFavoriteSalon(state, salonId))
    }

    fun spendFromWallet(amount: Int, sourceLabel: String, dateLabel: String) {
        dispatch(engine.spendFromWallet(state, amount, sourceLabel, dateLabel))
    }

    /** Customer Journey Audit (Booking Success P0): records a completed booking as a real appointment. */
    fun bookAppointment(
        salonName: String,
        serviceName: String,
        specialistName: String,
        serviceId: String,
        specialistId: String?,
        dateKey: String,
        dateLabel: String,
        time: String,
        price: Int,
    ) {
        dispatch(engine.bookAppointment(state, salonName, serviceName, specialistName, serviceId, specialistId, dateKey, dateLabel, time, price))
    }

    fun clearLastEvents() {
        lastEvents = emptyList()
    }

    // Category 2 (Deterministic Computations) pass-throughs - stateless
    // reads, but routed through the ViewModel rather than a second,
    // redundant Engine instance, since this ViewModel is already the
    // single path every one of these screens uses.
    fun allCoupons() = engine.allCoupons()
    fun allLoyaltyEntries() = engine.allLoyaltyEntries()
    fun membershipTier() = engine.membershipTier()
}
