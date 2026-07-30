package ai.rojan.designlab.manager.domain.appointment

/**
 * Manager Booking Journey Phase 2 — canonical week of selectable date
 * keys, shared by [ai.rojan.designlab.manager.screens.booking.ManagerBookingDateTimeScreen]
 * (date picker) and [ai.rojan.designlab.manager.screens.calendar.ManagerCalendarScreen]
 * (day selector), so an appointment created through the booking flow
 * uses the exact same date-key vocabulary Calendar already buckets by —
 * a single source of truth instead of two independently-drifting copies
 * of "the current week." No real calendar/date library is wired into
 * this codebase yet, so this remains a static reference week, same
 * convention as every other Manager sample data set.
 */
data class CalendarWeekDay(val key: String, val label: String, val dayNumber: String)

object ManagerCalendarWeek {
    val days = listOf(
        CalendarWeekDay("۱۴۰۴/۰۵/۰۴", "شنبه", "۲۵"),
        CalendarWeekDay("۱۴۰۴/۰۵/۰۵", "یکشنبه", "۲۶"),
        CalendarWeekDay("۱۴۰۴/۰۵/۰۶", "دوشنبه", "۲۷"),
        CalendarWeekDay("۱۴۰۴/۰۵/۰۷", "سه‌شنبه", "۲۸"),
        CalendarWeekDay("۱۴۰۴/۰۵/۰۸", "چهارشنبه", "۲۹"),
        CalendarWeekDay("۱۴۰۴/۰۵/۰۹", "پنجشنبه", "۳۰"),
        CalendarWeekDay("۱۴۰۴/۰۵/۱۰", "جمعه", "۱"),
    )

    /** First day of the reference week stands in for "today" — no real clock is wired in. */
    val todayKey: String = days.first().key

    fun labelFor(key: String): String = days.find { it.key == key }?.label ?: key
}
