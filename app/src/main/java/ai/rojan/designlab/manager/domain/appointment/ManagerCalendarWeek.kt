package ai.rojan.designlab.manager.domain.appointment

import ai.rojan.designlab.manager.data.toPersianDigits
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * [key] is Gregorian, formatted `yyyy/MM/dd` with ASCII digits — deliberately
 * the exact same format [ai.rojan.designlab.manager.data.BackendAppointmentRepository]
 * produces for [Appointment.date], so a real appointment's date always
 * matches a real calendar day key. [isoDate] is the same date in
 * `yyyy-MM-dd` form, for backend query params (`AvailabilityController`'s
 * `date: LocalDate`, Spring's default ISO parsing) — kept as a separate
 * field rather than reformatting [key] at every call site.
 *
 * [label]/[dayNumber] are Persian display only: [label] is a plain
 * weekday-name translation of the real [java.time.DayOfWeek] (not a
 * calendar-system conversion), and [dayNumber] is the real Gregorian
 * day-of-month in Persian digits — **not** the Jalali (Iranian solar)
 * calendar day number. No Jalali conversion library exists anywhere in
 * this codebase, and implementing one correctly (leap-year rules
 * included) was not this ticket's ask, which was specifically about real
 * *availability*, not full Jalali-calendar UI — see this class's own
 * history for why a full calendar system was never wired in. This keeps
 * [dayNumber] real and clock-driven either way, just not Jalali.
 *
 * Phase 2, M7: [days] became a real, clock-driven rolling 7-day window
 * (today + next six days) instead of a static hardcoded reference week —
 * that mismatch was a genuine bug, not just a cosmetic gap: real backend
 * appointments compare their [Appointment.date] against [ManagerCalendarWeek.todayKey]
 * by plain string equality (Calendar's day grouping, Dashboard's "today"
 * KPIs/[ai.rojan.designlab.manager.data.computeTodaysUpcomingSlots]), and a
 * fixed fake date never equalled any real appointment's actual date, so
 * those views silently showed nothing for real data (see
 * [ai.rojan.designlab.manager.data.BackendAppointmentRepository]'s own doc
 * comment, which flagged this exact gap when it made appointment data
 * real).
 */
data class CalendarWeekDay(val key: String, val label: String, val dayNumber: String, val isoDate: String)

private val weekdayLabels = mapOf(
    DayOfWeek.SATURDAY to "شنبه",
    DayOfWeek.SUNDAY to "یکشنبه",
    DayOfWeek.MONDAY to "دوشنبه",
    DayOfWeek.TUESDAY to "سه‌شنبه",
    DayOfWeek.WEDNESDAY to "چهارشنبه",
    DayOfWeek.THURSDAY to "پنجشنبه",
    DayOfWeek.FRIDAY to "جمعه",
)

/**
 * Final Release Validation — Real Booking Calendar Integration: real,
 * clock-driven week (today + the next 6 days), replacing the previously
 * static/fake reference week. Shared by
 * [ai.rojan.designlab.manager.screens.booking.ManagerBookingDateTimeScreen]
 * (date picker) and [ai.rojan.designlab.manager.screens.calendar.ManagerCalendarScreen]
 * (day selector) — a single source of truth, same as before, just real
 * now. [days] is a computed property (not a frozen `val`) so a session
 * spanning midnight sees a fresh "today" rather than a stale one baked in
 * at process start.
 */
object ManagerCalendarWeek {

    val days: List<CalendarWeekDay>
        get() {
            val today = LocalDate.now()
            return (0..6).map { offset ->
                val date = today.plusDays(offset.toLong())
                CalendarWeekDay(
                    key = date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                    label = weekdayLabels.getValue(date.dayOfWeek),
                    dayNumber = date.dayOfMonth.toString().toPersianDigits(),
                    isoDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                )
            }
        }

    /** Real today — [days] always starts on it (see [computeManagerDashboardStats]'s use of this for "today's appointments"). */
    val todayKey: String get() = days.first().key

    fun labelFor(key: String): String = days.find { it.key == key }?.label ?: key

    /** [CalendarWeekDay.isoDate] for a [key] outside the current [days] window (e.g. a persisted real [Appointment.date] from a prior day) — same format, no calendar lookup needed since both are plain Gregorian. */
    fun isoDateFor(key: String): String = key.replace('/', '-')
}
