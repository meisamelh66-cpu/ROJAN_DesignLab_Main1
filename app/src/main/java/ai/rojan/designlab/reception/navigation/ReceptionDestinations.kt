package ai.rojan.designlab.reception.navigation

/**
 * Route constants for the isolated Reception App workspace. Deliberately
 * separate from [ai.rojan.designlab.navigation.RojanDestinations] and
 * [ai.rojan.designlab.manager.navigation.ManagerDestinations] (both
 * untouched) — same "own destinations object per flavor" shape.
 *
 * Phase 0 (ROJAN_Reception_Implementation_Plan_v1.md): auth + salon
 * selection + a placeholder dashboard. Phase 1 (authentication completion,
 * approved per ROJAN_Reception_Phase1_Readiness_Report_v1.md): adds
 * [ACCESS_ERROR] (the resolved terminal state for a `/salon-access`
 * failure, previously unreachable — see
 * [ai.rojan.designlab.reception.presentation.auth.ReceptionAuthViewModel.refreshIdentityContext]'s
 * own doc comment) and [PROFILE]. Calendar/booking/customers routes are
 * still Phase 2+, not added here.
 */
object ReceptionDestinations {

    /** The gate's "not authenticated" destination — see [ai.rojan.designlab.manager.navigation.ManagerDestinations.OTP_AUTH]'s identical reasoning. */
    const val OTP_AUTH = "reception_otp_auth"

    /** Reached only from the gate when more than one salon is available and none was already validly selected. */
    const val SALON_SELECTION = "reception_salon_selection"

    /** Reached when salon-access resolution fails (network error, or the not-yet-real `/salon-access` endpoint) — a real, navigable dead end with retry/logout, not a silent hang. */
    const val ACCESS_ERROR = "reception_access_error"

    /** The real Reception Dashboard (System2 Reception Phase1 Controlled Implementation) — today's bookings + quick actions. Replaces the Phase 0 placeholder. */
    const val DASHBOARD = "reception_dashboard_root"

    /** Account info (name/phone) + the real logout action — reached from [DASHBOARD]. */
    const val PROFILE = "reception_profile"

    /** `VIEW_CRM`-scoped customer search — reached from [DASHBOARD]. */
    const val CUSTOMERS = "reception_customers"

    // Booking wizard — nested graph so every screen inside shares ONE
    // ReceptionBookingViewModel instance, scoped to this graph's own
    // back-stack entry (mirrors Manager's BOOKING_FLOW_GRAPH /
    // managerBookingViewModelFor in ManagerNavGraph.kt).
    const val BOOKING_FLOW_GRAPH = "reception_booking_flow_graph"
    const val CREATE_APPOINTMENT = "reception_booking_start"
    const val BOOKING_CUSTOMER = "reception_booking_customer"
    const val BOOKING_SERVICE = "reception_booking_service"
    const val BOOKING_SPECIALIST = "reception_booking_specialist"
    const val BOOKING_DATETIME = "reception_booking_datetime"
    const val BOOKING_REVIEW = "reception_booking_review"
    const val BOOKING_SUCCESS = "reception_booking_success"
}
