package ai.rojan.designlab.domain.booking

import java.util.Calendar

/**
 * Real rolling 7-day calendar window (today + next six days) for
 * [ai.rojan.designlab.screens.bookingflow.BookingDateScreen] — replaces
 * the fixed, hardcoded-Persian-date demo table this screen used before
 * the Android <-> Backend Full Integration milestone
 * (`ai.rojan.designlab.data.demo.DemoTimeSlotRepository.availableDates`,
 * literally "امروز، ۲۳ تیر" regardless of the real date). ISO dates
 * (`yyyy-MM-dd`) feed the backend's `available-slots` endpoint directly;
 * labels are Gregorian-calendar Persian text — this codebase has no
 * Jalali/Persian-calendar conversion utility or library, and adding one is
 * out of this milestone's scope, so "۲۳ تیر"-style Jalali labels aren't
 * reproduced here — a disclosed simplification, not a silent one.
 *
 * Uses [java.util.Calendar], not `java.time`, because this app's minSdk
 * (24) predates `java.time` without core library desugaring, which isn't
 * enabled in this project.
 */
object RollingBookingDates {

    private val weekdayNames = mapOf(
        Calendar.SATURDAY to "شنبه",
        Calendar.SUNDAY to "یکشنبه",
        Calendar.MONDAY to "دوشنبه",
        Calendar.TUESDAY to "سه‌شنبه",
        Calendar.WEDNESDAY to "چهارشنبه",
        Calendar.THURSDAY to "پنجشنبه",
        Calendar.FRIDAY to "جمعه",
    )

    private val monthNames = mapOf(
        Calendar.JANUARY to "ژانویه",
        Calendar.FEBRUARY to "فوریه",
        Calendar.MARCH to "مارس",
        Calendar.APRIL to "آوریل",
        Calendar.MAY to "می",
        Calendar.JUNE to "ژوئن",
        Calendar.JULY to "ژوئیه",
        Calendar.AUGUST to "اوت",
        Calendar.SEPTEMBER to "سپتامبر",
        Calendar.OCTOBER to "اکتبر",
        Calendar.NOVEMBER to "نوامبر",
        Calendar.DECEMBER to "دسامبر",
    )

    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    private fun toPersianDigits(value: Int): String =
        value.toString().map { ch -> if (ch.isDigit()) persianDigits[ch - '0'] else ch }.joinToString("")

    private fun isoDateFor(calendar: Calendar): String {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(year, month, day)
    }

    /** `(isoDate, displayLabel)` pairs for today through six days from now. */
    fun next7Days(): List<Pair<String, String>> {
        val calendar = Calendar.getInstance()
        val result = mutableListOf<Pair<String, String>>()
        for (offset in 0 until 7) {
            if (offset > 0) calendar.add(Calendar.DAY_OF_MONTH, 1)
            val isoDate = isoDateFor(calendar)
            val label = when (offset) {
                0 -> "امروز"
                1 -> "فردا"
                else -> {
                    val weekday = weekdayNames[calendar.get(Calendar.DAY_OF_WEEK)].orEmpty()
                    val day = toPersianDigits(calendar.get(Calendar.DAY_OF_MONTH))
                    val month = monthNames[calendar.get(Calendar.MONTH)].orEmpty()
                    "$weekday، $day $month"
                }
            }
            result += isoDate to label
        }
        return result
    }

    /** Display label for [isoDate] within the current rolling window, falling back to the raw key if it's outside it (e.g. a stale/edited selection). */
    fun labelFor(isoDate: String): String = next7Days().find { it.first == isoDate }?.second ?: isoDate
}
