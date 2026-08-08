package ai.rojan.designlab.manager.data

import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus
import ai.rojan.designlab.manager.domain.appointment.ManagerCalendarWeek
import ai.rojan.designlab.manager.domain.booking.managerBookingTimeSlots

/**
 * Manager Booking Journey Phase 2 — pure computation over
 * [ManagerRepositories], kept out of
 * [ai.rojan.designlab.manager.components.TodayOverviewSection] (a
 * Composable) per "no business logic inside Composables."
 *
 * Phase 2, M6: [todaysRevenueLabel]/[newCustomerCount] now come straight
 * from [ManagerRepositories.dashboardInsights] — the real, server-
 * computed `GET /dashboard/insights` — instead of being recomputed here
 * client-side (this used to sum today's active-appointment service
 * prices and count customers tagged [ai.rojan.designlab.manager.domain.customer.CustomerTag.NEW],
 * a client-invented definition of "new" that didn't match the backend's
 * real one). [todaysAppointmentCount]/[occupancyPercent] have no backend
 * equivalent (the insights endpoint reports monthly aggregates, not a
 * same-day appointment count or an occupancy ratio) and stay local:
 * [todaysAppointmentCount] is a plain count over already-synced real
 * appointment data (no computation to duplicate), and [occupancyPercent]
 * remains the same disclosed approximation as before — booked-slots /
 * total-capacity, where capacity = active specialists * the fixed daily
 * slot grid the booking flow itself offers — a genuine (if simplified)
 * ratio, not a fabricated number, and not something the backend computes
 * at all.
 */
data class ManagerDashboardStats(
    val todaysAppointmentCount: Int,
    val todaysRevenueLabel: String,
    val newCustomerCount: Int,
    val occupancyPercent: Int,
)

fun computeManagerDashboardStats(): ManagerDashboardStats {
    val appointments = ManagerRepositories.appointments.getAll()
    val specialists = ManagerRepositories.specialists.getAll()
    val insights = ManagerRepositories.dashboardInsights

    // No real clock/calendar is wired into this codebase yet — "today"
    // is ManagerCalendarWeek's reference-week convention, same one the
    // Calendar screen and booking DateTime picker both use.
    val todays = appointments.filter { it.date == ManagerCalendarWeek.todayKey }
    val todaysActive = todays.filter { it.status != AppointmentStatus.CANCELLED }

    // Occupancy is approximated as booked-slots / total-capacity, where
    // capacity = active specialists * the fixed daily slot grid the
    // booking flow itself offers — a genuine (if simplified) ratio, not
    // a fabricated number.
    val activeSpecialistCount = specialists.count { it.active }
    val totalCapacity = (activeSpecialistCount * managerBookingTimeSlots.size).coerceAtLeast(1)
    val occupancy = ((todaysActive.size * 100) / totalCapacity).coerceIn(0, 100)

    return ManagerDashboardStats(
        todaysAppointmentCount = todays.size,
        todaysRevenueLabel = insights?.let { formatRevenueLabel(it.todaysRevenue) } ?: "—",
        newCustomerCount = insights?.newCustomersThisMonth ?: 0,
        occupancyPercent = occupancy,
    )
}

private fun formatRevenueLabel(revenue: Double): String =
    if (revenue >= 1_000_000) {
        val millions = revenue / 1_000_000.0
        "%.1f".format(millions).toPersianDigits() + "م"
    } else {
        "${(revenue / 1000).toLong().toPersianDigits()}ت"
    }

/** A single row in [ai.rojan.designlab.manager.components.CalendarPreviewSection] — resolved display names, not raw ids. */
data class UpcomingSlot(val time: String, val clientName: String, val service: String)

/**
 * Real slice of today's non-cancelled appointments (Phase 2, M6) —
 * replaces [ai.rojan.designlab.manager.components.CalendarPreviewSection]'s
 * previous 3 hardcoded sample rows. Sourced entirely from data the full
 * Calendar screen already syncs ([ManagerRepositories.appointments]/
 * [ManagerRepositories.customers]/[ManagerRepositories.services]) - no
 * extra network call for the preview.
 */
fun computeTodaysUpcomingSlots(limit: Int = 3): List<UpcomingSlot> {
    val customers = ManagerRepositories.customers.getAll().associateBy { it.id }
    val services = ManagerRepositories.services.getAll().associateBy { it.id }

    return ManagerRepositories.appointments.getAll()
        .filter { it.date == ManagerCalendarWeek.todayKey && it.status != AppointmentStatus.CANCELLED }
        .sortedBy { it.time }
        .take(limit)
        .map { appointment ->
            UpcomingSlot(
                time = appointment.time,
                clientName = customers[appointment.customerId]?.name ?: "—",
                service = services[appointment.serviceId]?.name ?: "—",
            )
        }
}
