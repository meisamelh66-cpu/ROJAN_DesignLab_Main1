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
    val createdAppointmentId: String? = null,
    /**
     * TEAM2 Booking Creation Integrity follow-up. Non-null after every
     * [ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModel.confirm]
     * call — there is currently no backend contract that lets a salon
     * owner create a booking attributed to a different customer (see that
     * method's own doc comment), so confirming always fails honestly
     * rather than ever producing a local-only, silently-vanishing
     * "success." Kept on [ManagerBookingState] (not a separate
     * `MutableStateFlow`) — same single-source-of-truth shape every other
     * field here already follows.
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
