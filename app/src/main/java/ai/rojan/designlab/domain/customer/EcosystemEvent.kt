package ai.rojan.designlab.domain.customer

import ai.rojan.designlab.data.demo.DemoAppointment
import ai.rojan.designlab.data.demo.DemoBeautyTimelineEntry
import ai.rojan.designlab.data.demo.DemoWaitlistEntry
import ai.rojan.designlab.domain.notification.QueuedNotification

/**
 * A real, observable consequence of a customer action. This is the
 * single channel cross-module state changes flow through — per the
 * explicit "every cross-module interaction must go through Events
 * rather than direct module updates" requirement,
 * [CustomerEcosystemEngine] only ever *computes* a list of these; it
 * never mutates [CustomerEcosystemState] directly. [EcosystemEventReducer]
 * is the one place that actually applies an event to state.
 */
sealed interface EcosystemEvent {

    data class LoyaltyPointsEarned(
        val points: Int
    ) : EcosystemEvent

    data class WalletCashbackAdded(
        val amount: Int,
        val sourceLabel: String,
        val dateLabel: String
    ) : EcosystemEvent

    data class MembershipProgressUpdated(
        val incrementPoints: Int
    ) : EcosystemEvent

    data class BeautyTimelineEntryAdded(
        val entry: DemoBeautyTimelineEntry
    ) : EcosystemEvent

    data object ReviewRequestCreated : EcosystemEvent

    data class NotificationEnqueued(
        val notification: QueuedNotification
    ) : EcosystemEvent


    data class AppointmentStatusChanged(
        val appointmentId: String,
        val newStatus: ai.rojan.designlab.data.demo.AppointmentStatus,
        val newDaysAgo: Int?,
    ) : EcosystemEvent


    /**
     * Appointment System completion (V1.0 Module 6):
     * Reschedule changes date/time but keeps appointment status.
     */
    data class AppointmentRescheduled(
        val appointmentId: String,
        val newDateKey: String,
        val newDateLabel: String,
        val newTime: String,
    ) : EcosystemEvent


    // Appointment System completion (V1.0 Module 6 - Waiting List)

    data class WaitlistJoined(
        val entry: DemoWaitlistEntry
    ) : EcosystemEvent


    data class WaitlistJoinRejected(
        val reason: String
    ) : EcosystemEvent


    data class WaitlistLeft(
        val entryId: String
    ) : EcosystemEvent


    data class WaitlistPromoted(
        val entryId: String,
        val newAppointment: DemoAppointment,
    ) : EcosystemEvent


    /** Customer Journey Audit (Booking Success P0): a completed booking becomes a real, visible appointment. */
    data class AppointmentBooked(
        val appointment: DemoAppointment,
    ) : EcosystemEvent


    data class CouponRedeemed(
        val couponId: String,
        val discountAmount: Int
    ) : EcosystemEvent


    data class CouponRejected(
        val reason: CouponRejectionReason
    ) : EcosystemEvent


    data class ReviewLifecycleAdvanced(
        val appointmentId: String,
        val newStatus: ReviewLifecycleStatus
    ) : EcosystemEvent


    data class ReviewSubmitted(
        val review: ai.rojan.designlab.data.demo.DemoUserReview
    ) : EcosystemEvent


    data class ReviewRejected(
        val reason: ReviewRejectionReason
    ) : EcosystemEvent


    data class FavoriteSalonToggled(
        val salonId: String,
        val isNowFavorite: Boolean
    ) : EcosystemEvent


    data class WalletDebited(
        val amount: Int,
        val sourceLabel: String,
        val dateLabel: String
    ) : EcosystemEvent


    data object WalletDebitRejected : EcosystemEvent
}


enum class CouponRejectionReason {
    ALREADY_USED,
    EXPIRED
}


enum class ReviewRejectionReason {
    DUPLICATE_FOR_APPOINTMENT,
    INVALID_LIFECYCLE_STATE
}