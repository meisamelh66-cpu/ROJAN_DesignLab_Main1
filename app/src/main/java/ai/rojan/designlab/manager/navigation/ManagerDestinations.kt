package ai.rojan.designlab.manager.navigation

/**
 * Route constants for the isolated Manager App workspace. Deliberately
 * separate from [ai.rojan.designlab.navigation.RojanDestinations] (which
 * stays untouched) — this module is not wired into the shared app nav
 * graph yet, only foundation for it.
 */
object ManagerDestinations {
    const val SPLASH = "manager_splash"
    /** TEAM2-002: the Manager app's own login gate — reuses the existing `AuthScreen`/`AuthViewModel` (real email/password login), not a redesigned screen. */
    const val LOGIN = "manager_login"
    const val DASHBOARD = "manager_dashboard_root"
    const val CALENDAR = "manager_calendar"
    const val CUSTOMERS = "manager_customers"
    const val CUSTOMER_PROFILE = "manager_customer_profile/{customerId}"
    fun customerProfile(customerId: String) = "manager_customer_profile/$customerId"
    const val SERVICES = "manager_services"
    const val STAFF = "manager_staff"
    const val SETTINGS = "manager_settings"
    const val PROFILE = "manager_profile"

    // Booking Journey Phase 2 — "نوبت جدید" quick action's flow. Nested
    // graph (mirrors Customer's RojanDestinations.BOOKING_FLOW_GRAPH) so
    // every screen inside shares one ManagerBookingViewModel instance,
    // scoped to this graph's own back-stack entry.
    const val BOOKING_FLOW_GRAPH = "manager_booking_flow_graph"
    const val CREATE_APPOINTMENT = "manager_booking_start"
    const val BOOKING_CUSTOMER = "manager_booking_customer"
    const val BOOKING_SERVICE = "manager_booking_service"
    const val BOOKING_SPECIALIST = "manager_booking_specialist"
    const val BOOKING_DATETIME = "manager_booking_datetime"
    const val BOOKING_REVIEW = "manager_booking_review"
    const val BOOKING_SUCCESS = "manager_booking_success"
}
