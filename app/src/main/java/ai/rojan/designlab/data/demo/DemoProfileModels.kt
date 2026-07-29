package ai.rojan.designlab.data.demo

import ai.rojan.designlab.domain.booking.PaymentMethod

/** Journey 2 (Profile) demo data models — same lightweight, no-persistence approach as Journey 1's DemoModels.kt. */

enum class AppointmentStatus { UPCOMING, COMPLETED, CANCELLED }

data class DemoAppointment(
    val id: String,
    val salonName: String,
    val serviceName: String,
    val specialistName: String,
    val dateLabel: String,
    val time: String,
    val status: AppointmentStatus,
    val price: Int,
    val relatedServiceId: String? = null,
    val daysAgo: Int? = null,
    /** Appointment System completion (V1.0 Module 6 - Reschedule): needed for real per-specialist availability checking via BookingEngine when picking a new date/time. Nullable since existing demo entries predate this field. */
    val specialistId: String? = null,
    /** Appointment System completion (V1.0 Module 6 - Waiting List): the machine-matchable date key (e.g. "today") behind [dateLabel]'s display string - needed to check this exact date against BookingEngine/waitlist entries without fragile label-string matching. */
    val dateKey: String? = null,
    /** UX Refactor Phase 1: lets "Previous Salons" (Customer Home) navigate to the actual salon rather than just displaying [salonName] as text. Nullable since existing demo entries predate this field. */
    val salonId: String? = null,
    /** Customer Journey Audit Phase A (P0-2): the payment method actually selected on Booking Confirmation — previously chosen in the UI but never recorded anywhere. Nullable since existing demo entries predate this field. */
    val paymentMethod: PaymentMethod? = null,
)

data class DemoWalletTransaction(
    val id: String,
    val title: String,
    val amount: Int,
    val isCredit: Boolean,
    val dateLabel: String,
)

data class DemoCoupon(
    val id: String,
    val title: String,
    val description: String,
    val discountPercent: Int,
    val expiryLabel: String,
    val code: String,
)

data class DemoMembershipTier(
    val currentTierName: String,
    val benefits: List<String>,
    val pointsToNextTier: Int,
    val nextTierName: String,
)

data class DemoLoyaltyEntry(
    val id: String,
    val title: String,
    val points: Int,
    val isEarned: Boolean,
    val dateLabel: String,
)

data class DemoBeautyTimelineEntry(
    val id: String,
    val serviceName: String,
    val salonName: String,
    val dateLabel: String,
    val note: String,
)

data class DemoUserReview(
    val id: String,
    val salonName: String,
    val rating: String,
    val comment: String,
    val dateLabel: String,
)
