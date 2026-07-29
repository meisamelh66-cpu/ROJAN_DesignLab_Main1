package ai.rojan.designlab.manager.navigation

/**
 * Route constants for the isolated Manager App workspace. Deliberately
 * separate from [ai.rojan.designlab.navigation.RojanDestinations] (which
 * stays untouched) — this module is not wired into the shared app nav
 * graph yet, only foundation for it.
 */
object ManagerDestinations {
    const val DASHBOARD = "manager_dashboard_root"
    const val CALENDAR = "manager_calendar"
    const val CUSTOMERS = "manager_customers"
    const val SERVICES = "manager_services"
    const val STAFF = "manager_staff"
    const val SETTINGS = "manager_settings"
}
