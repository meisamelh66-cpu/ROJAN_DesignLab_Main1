package ai.rojan.designlab.domain.customer

import ai.rojan.designlab.data.demo.AppointmentStatus
import ai.rojan.designlab.data.demo.DemoAppointment
import ai.rojan.designlab.data.demo.DemoBeautyTimelineEntry
import ai.rojan.designlab.data.demo.DemoCoupon
import ai.rojan.designlab.data.demo.DemoUserReview
import ai.rojan.designlab.domain.booking.BookingEngine
import ai.rojan.designlab.domain.booking.PaymentMethod
import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.domain.customer.rules.CashbackRuleProvider
import ai.rojan.designlab.domain.customer.rules.CouponEligibilityEngine
import ai.rojan.designlab.domain.customer.rules.CouponEligibilityResult
import ai.rojan.designlab.domain.customer.rules.LoyaltyRuleProvider
import ai.rojan.designlab.domain.customer.rules.MembershipProgressRuleProvider
import ai.rojan.designlab.domain.customer.rules.PlaceholderCashbackRuleProvider
import ai.rojan.designlab.domain.customer.rules.PlaceholderLoyaltyRuleProvider
import ai.rojan.designlab.domain.customer.rules.PlaceholderMembershipProgressRuleProvider
import ai.rojan.designlab.domain.notification.NotificationType
import ai.rojan.designlab.domain.notification.QueuedNotification
import ai.rojan.designlab.domain.waitlist.WaitlistEngine

/**
 * Customer ecosystem business rules — pure Kotlin, same discipline as
 * [ai.rojan.designlab.domain.booking.BookingEngine] (zero Compose/
 * Android/Navigation dependency).
 *
 * **Architecture correction from the previous revision:** every method
 * here now *only computes* a `List<EcosystemEvent>` — it never returns
 * or mutates [CustomerEcosystemState] directly. [EcosystemEventReducer]
 * is solely responsible for applying events to state. This is what
 * "every cross-module interaction must go through Events rather than
 * direct module updates" means as an enforced structure, not just a
 * description — the previous revision computed a new state directly
 * inside this class, which is exactly what this correction removes.
 *
 * Every numeric business rule (loyalty formula, cashback formula,
 * membership progress formula) is now behind an injected provider
 * interface defaulting to a `Placeholder*` implementation — each
 * explicitly documented as a temporary illustrative value, not an
 * approved business decision, per "never hardcode business decisions
 * that may later conflict with the Architecture Book." Swapping in real
 * BOOK 3 rules later means providing a real implementation of these
 * three interfaces at construction time — zero changes to this class,
 * the reducer, the ViewModel, or any screen.
 */
class CustomerEcosystemEngine(
    private val loyaltyRuleProvider: LoyaltyRuleProvider = PlaceholderLoyaltyRuleProvider(),
    private val cashbackRuleProvider: CashbackRuleProvider = PlaceholderCashbackRuleProvider(),
    private val membershipProgressRuleProvider: MembershipProgressRuleProvider = PlaceholderMembershipProgressRuleProvider(),
    private val couponEligibilityEngine: CouponEligibilityEngine = CouponEligibilityEngine(),
    private val waitlistEngine: WaitlistEngine = WaitlistEngine(),
    private val bookingEngine: BookingEngine = BookingEngine(),
    private val catalogEngine: CatalogEngine = CatalogEngine(),
) {

    /**
     * The flagship scenario: Appointment Completed → Beauty Timeline →
     * Loyalty → Wallet → Membership → Notification → Review Request.
     * "AI recommendation generated" from the original spec's example
     * chain is deliberately not included as an event here — generating
     * one would require calling [ai.rojan.designlab.domain.ai.AiRecommendationProvider],
     * which today only has a no-op implementation; emitting a fake
     * recommendation event for nothing real to have produced would be
     * exactly the "fake AI" this phase's instructions rule out.
     */
    fun completeAppointment(
        state: CustomerEcosystemState,
        appointmentId: String,
    ): List<EcosystemEvent> {
        val appointment = state.appointments.find { it.id == appointmentId } ?: return emptyList()

        val loyaltyPoints = loyaltyRuleProvider.calculatePointsEarned(appointment.price)
        val cashback = cashbackRuleProvider.calculateCashback(appointment.price)
        val membershipIncrement = membershipProgressRuleProvider.calculateProgressIncrement(loyaltyPoints)

        val timelineEntry = DemoBeautyTimelineEntry(
            id = "tl_${appointmentId}",
            serviceName = appointment.serviceName,
            salonName = appointment.salonName,
            dateLabel = appointment.dateLabel,
            note = "ثبت‌شده خودکار پس از تکمیل نوبت",
        )

        val notification = QueuedNotification(
            id = "notif_${appointmentId}",
            type = NotificationType.APPOINTMENT_COMPLETED,
            relatedEntityId = appointmentId,
            createdAtLabel = appointment.dateLabel,
        )

        return listOf(
            EcosystemEvent.AppointmentStatusChanged(appointmentId, AppointmentStatus.COMPLETED, newDaysAgo = 0),
            EcosystemEvent.BeautyTimelineEntryAdded(timelineEntry),
            EcosystemEvent.LoyaltyPointsEarned(loyaltyPoints),
            EcosystemEvent.WalletCashbackAdded(cashback, "بازگشت وجه - ${appointment.salonName}", appointment.dateLabel),
            EcosystemEvent.MembershipProgressUpdated(membershipIncrement),
            EcosystemEvent.NotificationEnqueued(notification),
            EcosystemEvent.ReviewRequestCreated,
            EcosystemEvent.ReviewLifecycleAdvanced(appointmentId, ReviewLifecycleStatus.REQUESTED),
        )
    }

    /**
     * Appointment System completion (V1.0 Module 6 - Cancel). Only
     * meaningful for an [AppointmentStatus.UPCOMING] appointment - a
     * completed or already-cancelled one returns no events (guard,
     * mirroring [completeAppointment]'s `?: return emptyList()`
     * pattern). Deliberately produces no loyalty/cashback/timeline
     * events, unlike [completeAppointment] - there is nothing to
     * reward for a cancellation.
     *
     * Waiting List integration: cancelling a real, dated, specialist-
     * assigned, service-linked appointment genuinely checks the
     * waitlist for that exact salon+service+specialist+date and
     * promotes the earliest FIFO match if [BookingEngine] confirms a
     * real slot is now available - "Automatic promotion when a time
     * slot becomes available" as an actual consequence of cancelling,
     * not a separate manual step. Silently skips the check
     * (cancellation still succeeds) when the appointment lacks the data
     * needed for it (older demo entries without
     * [ai.rojan.designlab.data.demo.DemoAppointment.dateKey]/
     * [ai.rojan.designlab.data.demo.DemoAppointment.specialistId]/
     * [ai.rojan.designlab.data.demo.DemoAppointment.relatedServiceId]) -
     * disclosed limitation, not a silent wrong guess.
     */
    fun cancelAppointment(
        state: CustomerEcosystemState,
        appointmentId: String,
    ): List<EcosystemEvent> {
        val appointment = state.appointments.find { it.id == appointmentId } ?: return emptyList()
        if (appointment.status != AppointmentStatus.UPCOMING) return emptyList()

        val notification = QueuedNotification(
            id = "notif_cancel_${appointmentId}",
            type = NotificationType.APPOINTMENT_CANCELLED,
            relatedEntityId = appointmentId,
            createdAtLabel = appointment.dateLabel,
        )

        val cancellationEvents = listOf(
            EcosystemEvent.AppointmentStatusChanged(appointmentId, AppointmentStatus.CANCELLED, newDaysAgo = appointment.daysAgo),
            EcosystemEvent.NotificationEnqueued(notification),
        )

        val serviceId = appointment.relatedServiceId
        val specialistId = appointment.specialistId
        val dateKey = appointment.dateKey
        val promotionEvents = if (serviceId != null && specialistId != null && dateKey != null) {
            val service = catalogEngine.findServiceById(serviceId)
            val salon = service?.let { catalogEngine.findSalonById(it.salonId) }
            val specialist = catalogEngine.findSpecialistById(specialistId)
            if (service != null && salon != null && specialist != null) {
                waitlistEngine.checkForPromotion(
                    state = state,
                    bookingEngine = bookingEngine,
                    salonId = salon.id,
                    salonName = salon.name,
                    serviceId = serviceId,
                    serviceName = service.name,
                    freedSpecialistId = specialistId,
                    freedSpecialistName = specialist.name,
                    dateKey = dateKey,
                    durationMinutes = service.durationMinutes,
                    priceForNewAppointment = service.discountPrice ?: service.price,
                )
            } else emptyList()
        } else emptyList()

        return cancellationEvents + promotionEvents
    }

    /**
     * Appointment System completion (V1.0 Module 6 - Reschedule). Only
     * meaningful for an [AppointmentStatus.UPCOMING] appointment,
     * matching [cancelAppointment]'s guard. Status is untouched -
     * rescheduling changes when the appointment happens, not whether
     * it's still upcoming.
     */
    fun rescheduleAppointment(
        state: CustomerEcosystemState,
        appointmentId: String,
        newDateKey: String,
        newDateLabel: String,
        newTime: String,
    ): List<EcosystemEvent> {
        val appointment = state.appointments.find { it.id == appointmentId } ?: return emptyList()
        if (appointment.status != AppointmentStatus.UPCOMING) return emptyList()

        val notification = QueuedNotification(
            id = "notif_reschedule_${appointmentId}",
            type = NotificationType.APPOINTMENT_RESCHEDULED,
            relatedEntityId = appointmentId,
            createdAtLabel = newDateLabel,
        )

        return listOf(
            EcosystemEvent.AppointmentRescheduled(appointmentId, newDateKey, newDateLabel, newTime),
            EcosystemEvent.NotificationEnqueued(notification),
        )
    }

    // ── Appointment System completion (V1.0 Module 6 - Waiting List) ──

    fun joinWaitlist(
        state: CustomerEcosystemState,
        salonId: String,
        salonName: String,
        serviceId: String,
        serviceName: String,
        specialistId: String?,
        dateKey: String,
        dateLabel: String,
    ): List<EcosystemEvent> = waitlistEngine.join(state, salonId, salonName, serviceId, serviceName, specialistId, dateKey, dateLabel)

    fun leaveWaitlist(state: CustomerEcosystemState, entryId: String): List<EcosystemEvent> =
        waitlistEngine.leave(state, entryId)

    /** Eligibility is checked first, via [CouponEligibilityEngine] — a separate step, not folded into this method's own logic. */
    fun redeemCoupon(
        state: CustomerEcosystemState,
        coupon: DemoCoupon,
        referencePrice: Int,
    ): List<EcosystemEvent> {
        return when (val eligibility = couponEligibilityEngine.check(state, coupon)) {
            is CouponEligibilityResult.Ineligible -> listOf(EcosystemEvent.CouponRejected(eligibility.reason))
            CouponEligibilityResult.Eligible -> {
                val discount = (referencePrice * coupon.discountPercent) / 100
                listOf(EcosystemEvent.CouponRedeemed(coupon.id, discount))
            }
        }
    }

    /** First stage: Pending Review → Review Request. Only valid from [ReviewLifecycleStatus.PENDING_REQUEST] or no prior record. */
    fun requestReview(state: CustomerEcosystemState, appointmentId: String): List<EcosystemEvent> {
        val current = state.pendingReviewFor(appointmentId)
        if (current != null && current.status != ReviewLifecycleStatus.PENDING_REQUEST) {
            return listOf(EcosystemEvent.ReviewRejected(ReviewRejectionReason.INVALID_LIFECYCLE_STATE))
        }
        return listOf(EcosystemEvent.ReviewLifecycleAdvanced(appointmentId, ReviewLifecycleStatus.REQUESTED))
    }

    /** Submit: Review Request → Submitted. Only valid from [ReviewLifecycleStatus.REQUESTED]; rejects a duplicate for an appointment already reviewed. */
    fun submitReview(
        state: CustomerEcosystemState,
        appointmentId: String,
        review: DemoUserReview,
    ): List<EcosystemEvent> {
        val current = state.pendingReviewFor(appointmentId)
        if (current == null || current.status != ReviewLifecycleStatus.REQUESTED) {
            return listOf(EcosystemEvent.ReviewRejected(ReviewRejectionReason.INVALID_LIFECYCLE_STATE))
        }
        return listOf(
            EcosystemEvent.ReviewSubmitted(review),
            EcosystemEvent.ReviewLifecycleAdvanced(appointmentId, ReviewLifecycleStatus.SUBMITTED),
        )
    }

    /**
     * Publish: Submitted → Published. Kept as a separate, explicit call
     * rather than automatically chained after submit — a real
     * moderation/approval step (if BOOK 3 specifies one) would call this
     * independently; auto-publishing immediately would be guessing that
     * no such step exists.
     */
    fun publishReview(state: CustomerEcosystemState, appointmentId: String): List<EcosystemEvent> {
        val current = state.pendingReviewFor(appointmentId)
        if (current == null || current.status != ReviewLifecycleStatus.SUBMITTED) {
            return listOf(EcosystemEvent.ReviewRejected(ReviewRejectionReason.INVALID_LIFECYCLE_STATE))
        }
        return listOf(EcosystemEvent.ReviewLifecycleAdvanced(appointmentId, ReviewLifecycleStatus.PUBLISHED))
    }

    fun toggleFavoriteSalon(state: CustomerEcosystemState, salonId: String): List<EcosystemEvent> {
        val isCurrentlyFavorite = salonId in state.favoriteSalonIds
        return listOf(EcosystemEvent.FavoriteSalonToggled(salonId, isNowFavorite = !isCurrentlyFavorite))
    }

    /** Negative wallet balance prevention — a real, enforced edge case, not just documented. */
    fun spendFromWallet(state: CustomerEcosystemState, amount: Int, sourceLabel: String, dateLabel: String): List<EcosystemEvent> {
        if (amount > state.walletBalance) return listOf(EcosystemEvent.WalletDebitRejected)
        return listOf(EcosystemEvent.WalletDebited(amount, sourceLabel, dateLabel))
    }

    /**
     * Customer Journey Audit (Booking Success P0): turns a completed
     * [ai.rojan.designlab.domain.booking.BookingState] into a real, visible
     * [DemoAppointment] — same shape as [WaitlistEngine.checkForPromotion]'s
     * own [DemoAppointment] construction, reusing the existing model
     * rather than inventing a parallel one. ID scheme matches the
     * existing wallet-transaction convention (list size + a distinguishing
     * hash) since, unlike a promoted waitlist entry, a fresh booking has
     * no prior entity ID to derive from.
     */
    fun bookAppointment(
        state: CustomerEcosystemState,
        salonName: String,
        serviceName: String,
        specialistName: String,
        serviceId: String,
        specialistId: String?,
        dateKey: String,
        dateLabel: String,
        time: String,
        price: Int,
        salonId: String? = null,
        paymentMethod: PaymentMethod? = null,
        backendBookingId: String? = null,
    ): List<EcosystemEvent> {
        val appointment = DemoAppointment(
            id = "appt_${state.appointments.size}_${serviceId.hashCode()}",
            salonName = salonName,
            serviceName = serviceName,
            specialistName = specialistName,
            dateLabel = dateLabel,
            time = time,
            status = AppointmentStatus.UPCOMING,
            price = price,
            relatedServiceId = serviceId,
            specialistId = specialistId,
            dateKey = dateKey,
            salonId = salonId,
            paymentMethod = paymentMethod,
            backendBookingId = backendBookingId,
        )
        return listOf(EcosystemEvent.AppointmentBooked(appointment))
    }

    // ── Category 2 (Deterministic Computations) — simple static catalog
    // reads within the Customer domain. No Rule Provider (not a judgment
    // call), no Events/Reducer (nothing mutated) - same reasoning as
    // CatalogEngine, kept here rather than a separate class since these
    // are Customer-domain data, not Booking/discovery-domain.

    fun allCoupons(): List<ai.rojan.designlab.data.demo.DemoCoupon> =
        ai.rojan.designlab.data.demo.DemoCouponRepository.coupons

    fun allLoyaltyEntries(): List<ai.rojan.designlab.data.demo.DemoLoyaltyEntry> =
        ai.rojan.designlab.data.demo.DemoLoyaltyRepository.entries

    fun membershipTier(): ai.rojan.designlab.data.demo.DemoMembershipTier =
        ai.rojan.designlab.data.demo.DemoMembershipRepository.tier
}
