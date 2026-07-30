package ai.rojan.designlab.manager.data

import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus
import ai.rojan.designlab.manager.domain.appointment.ManagerCalendarWeek
import ai.rojan.designlab.manager.domain.booking.managerBookingTimeSlots
import ai.rojan.designlab.manager.domain.customer.CustomerTag

/**
 * Manager Booking Journey Phase 2 — pure computation over
 * [ManagerRepositories], kept out of
 * [ai.rojan.designlab.manager.components.TodayOverviewSection] (a
 * Composable) per "no business logic inside Composables."
 */
data class ManagerDashboardStats(
    val todaysAppointmentCount: Int,
    val todaysRevenueLabel: String,
    val newCustomerCount: Int,
    val occupancyPercent: Int,
)

fun computeManagerDashboardStats(): ManagerDashboardStats {
    val appointments = ManagerRepositories.appointments.getAll()
    val services = ManagerRepositories.services.getAll().associateBy { it.id }
    val specialists = ManagerRepositories.specialists.getAll()
    val customers = ManagerRepositories.customers.getAll()

    // No real clock/calendar is wired into this codebase yet — "today"
    // is ManagerCalendarWeek's reference-week convention, same one the
    // Calendar screen and booking DateTime picker both use.
    val todays = appointments.filter { it.date == ManagerCalendarWeek.todayKey }
    val todaysActive = todays.filter { it.status != AppointmentStatus.CANCELLED }

    val revenue = todaysActive.sumOf { services[it.serviceId]?.price ?: 0L }

    // Occupancy is approximated as booked-slots / total-capacity, where
    // capacity = active specialists * the fixed daily slot grid the
    // booking flow itself offers — a genuine (if simplified) ratio, not
    // a fabricated number.
    val activeSpecialistCount = specialists.count { it.active }
    val totalCapacity = (activeSpecialistCount * managerBookingTimeSlots.size).coerceAtLeast(1)
    val occupancy = ((todaysActive.size * 100) / totalCapacity).coerceIn(0, 100)

    return ManagerDashboardStats(
        todaysAppointmentCount = todays.size,
        todaysRevenueLabel = formatRevenueLabel(revenue),
        newCustomerCount = customers.count { it.tag == CustomerTag.NEW },
        occupancyPercent = occupancy,
    )
}

private fun formatRevenueLabel(revenue: Long): String =
    if (revenue >= 1_000_000) {
        val millions = revenue / 1_000_000.0
        "%.1f".format(millions).toPersianDigits() + "م"
    } else {
        "${(revenue / 1000).toPersianDigits()}ت"
    }
