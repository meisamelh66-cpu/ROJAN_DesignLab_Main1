package ai.rojan.designlab.domain.customer

import ai.rojan.designlab.data.demo.AppointmentStatus
import ai.rojan.designlab.data.demo.DemoAppointment
import ai.rojan.designlab.data.demo.DemoAppointmentRepository
import ai.rojan.designlab.data.demo.DemoBeautyTimelineEntry
import ai.rojan.designlab.data.demo.DemoBeautyTimelineRepository
import ai.rojan.designlab.data.demo.DemoLoyaltyRepository
import ai.rojan.designlab.data.demo.DemoSalonRepository
import ai.rojan.designlab.data.demo.DemoUserReview
import ai.rojan.designlab.data.demo.DemoUserReviewRepository
import ai.rojan.designlab.data.demo.DemoWaitlistEntry
import ai.rojan.designlab.data.demo.DemoWalletRepository
import ai.rojan.designlab.data.demo.DemoWalletTransaction
import ai.rojan.designlab.data.demo.WaitlistStatus
import ai.rojan.designlab.domain.notification.NotificationQueue
import ai.rojan.designlab.domain.reminder.DemoReminderPreference

/**
 * The customer ecosystem's real, mutable-via-copy shared state. Every
 * field that item 12 ("do not leave any static demo lists where mutable
 * shared state should exist") applies to now lives here — Beauty
 * Timeline and Favorites included, not just Wallet/Loyalty/Membership
 * from the prior pass.
 *
 * Initial values seed from the existing static repositories (the bridge
 * between the old read-only data layer and this one — not a duplicate
 * source of truth; once seeded, screens read only from here).
 *
 * Appointment System completion (V1.0 Module 6): [waitlistEntries] +
 * [waitlistSequenceCounter] (Waiting List, FIFO ordering source) and
 * [reminderPreferences] (Reminder domain) added, same single-source-of-
 * truth pattern as everything else here.
 */
data class CustomerEcosystemState(
    val walletBalance: Int = DemoWalletRepository.BALANCE,
    val pendingBalance: Int = 0,
    val walletTransactions: List<DemoWalletTransaction> = DemoWalletRepository.transactions,
    val loyaltyPoints: Int = DemoLoyaltyRepository.TOTAL_POINTS,
    val membershipProgressPoints: Int = 0,
    val appointments: List<DemoAppointment> = DemoAppointmentRepository.appointments,
    val usedCouponIds: Set<String> = emptySet(),
    val reviews: List<DemoUserReview> = DemoUserReviewRepository.reviews,
    val pendingReviews: List<PendingReview> = emptyList(),
    val beautyTimelineEntries: List<DemoBeautyTimelineEntry> = DemoBeautyTimelineRepository.entries,
    val favoriteSalonIds: Set<String> = DemoSalonRepository.salons.map { it.id }.toSet(),
    val notificationQueue: NotificationQueue = NotificationQueue(),
    val customerBirthday: String = "۱۲ مرداد",
    val waitlistEntries: List<DemoWaitlistEntry> = emptyList(),
    val waitlistSequenceCounter: Int = 1,
    val reminderPreferences: List<DemoReminderPreference> = emptyList(),
) {
    val upcomingAppointments: List<DemoAppointment>
        get() = appointments.filter { it.status == AppointmentStatus.UPCOMING }

    val pastAppointments: List<DemoAppointment>
        get() = appointments.filter { it.status != AppointmentStatus.UPCOMING }

    fun pendingReviewFor(appointmentId: String): PendingReview? =
        pendingReviews.find { it.appointmentId == appointmentId }

    /** Appointment System completion (V1.0 Module 6 - Waiting List). */
    val activeWaitlistEntries: List<DemoWaitlistEntry>
        get() = waitlistEntries.filter { it.status == WaitlistStatus.WAITING }

    /** Appointment System completion (V1.0 Module 6 - Reminder). */
    fun reminderPreferenceFor(appointmentId: String): DemoReminderPreference? =
        reminderPreferences.find { it.appointmentId == appointmentId }
}
