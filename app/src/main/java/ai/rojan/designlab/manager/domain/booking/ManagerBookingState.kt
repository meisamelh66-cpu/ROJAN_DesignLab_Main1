package ai.rojan.designlab.manager.domain.booking

/**
 * Manager Booking Journey Phase 2 — immutable snapshot of an
 * in-progress manager-created appointment. A new, right-sized type
 * rather than reusing [ai.rojan.designlab.domain.booking.BookingState]
 * (the Customer domain's booking session): that class carries
 * `promotionId`/`couponId`/`paymentMethod`/`intent`, none of which apply
 * here, and has no `customerId` slot at all (the Customer flow never
 * needs one — the app already knows who's logged in), whereas a manager
 * booking on someone's behalf needs exactly that. The *pattern*
 * (immutable state, single source of truth, mutated only through the
 * owning ViewModel) is reused; the concrete fields are not.
 */
data class ManagerBookingState(
    val customerId: String? = null,
    val serviceId: String? = null,
    val specialistId: String? = null,
    val dateKey: String? = null,
    val time: String? = null,
    val isSubmitting: Boolean = false,
    /** The real backend `Booking.id` once [ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModel.confirm] genuinely succeeds — non-null only then, never for a local-only "success." */
    val createdAppointmentId: String? = null,
    /**
     * Manager Booking Creation Integrity follow-up. Non-null exactly when
     * the most recent [ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModel.confirm]
     * attempt did not produce a persisted backend booking — a real
     * backend/network/validation failure now that the backend contract
     * for booking on a customer's behalf exists, not a permanent
     * structural block any more. Kept on [ManagerBookingState] (not a
     * separate `MutableStateFlow`) — same single-source-of-truth shape
     * every other field here already follows.
     */
    val submitError: String? = null,
) {
    val isReadyToConfirm: Boolean
        get() = customerId != null && serviceId != null && specialistId != null && dateKey != null && time != null
}

/** Fixed daily slot grid — no real scheduling engine wired in yet (Phase 1/2 foundation, per the Specialist model's own disclosed simplification). */
val managerBookingTimeSlots: List<String> = listOf(
    "۰۹:۰۰", "۰۹:۳۰", "۱۰:۰۰", "۱۰:۳۰", "۱۱:۰۰", "۱۱:۳۰",
    "۱۲:۰۰", "۱۲:۳۰", "۱۳:۰۰", "۱۳:۳۰", "۱۴:۰۰", "۱۴:۳۰",
    "۱۵:۰۰", "۱۵:۳۰", "۱۶:۰۰", "۱۶:۳۰", "۱۷:۰۰", "۱۷:۳۰",
)
