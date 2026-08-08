package ai.rojan.designlab.manager.navigation

/**
 * Route constants for the isolated Manager App workspace. Deliberately
 * separate from [ai.rojan.designlab.navigation.RojanDestinations] (which
 * stays untouched).
 *
 * [OTP_AUTH] (OTP Authentication Entry Flow Integration): the real backend
 * phone + OTP login screen ([ai.rojan.designlab.manager.screens.auth.ManagerOtpAuthScreen]),
 * registered by [ai.rojan.designlab.manager.navigation.ManagerNavGraph]
 * (the top-level gate composable) directly, not inside [managerNavGraph]
 * itself — it is reached only before a session exists, never navigated to
 * from within the authenticated app.
 */
object ManagerDestinations {
    /** OTP Authentication Entry Flow Integration — the gate's "not authenticated" destination. Splash is no longer a NavHost route (see `ManagerRootGraph.kt`): it's shown by the gate itself, before this NavHost is even created, so its startDestination can be chosen correctly (Dashboard vs. this) instead of always starting at a splash route. */
    const val OTP_AUTH = "manager_otp_auth"
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
