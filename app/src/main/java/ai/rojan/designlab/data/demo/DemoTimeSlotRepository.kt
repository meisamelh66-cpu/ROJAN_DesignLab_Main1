package ai.rojan.designlab.data.demo

/**
 * Booking Availability Architecture Correction: this repository now
 * provides *only* raw data. It used to also decide which slots were
 * unavailable (`private val unavailable = setOf(...)`) — a real
 * business decision sitting inside Infrastructure, which is exactly
 * what this correction removes. Availability is now
 * [ai.rojan.designlab.domain.booking.rules.BookingAvailabilityRules]'
 * job exclusively; this object has no opinion on which slots are
 * bookable, only which slots exist.
 */
object DemoTimeSlotRepository {

    /** Every time slot that exists, with no availability judgment attached. */
    val allSlotTimes: List<String> = listOf(
        "10:00", "10:30", "11:00", "11:30", "12:00",
        "14:00", "14:30", "15:00", "15:30", "16:00", "16:30",
    )

    /** Booking Engine completion: parses "HH:mm" to minutes since midnight - the machine-usable form [ai.rojan.designlab.domain.booking.rules.BookingAvailabilityRules] needs for real interval math. */
    fun toStartMinutes(time: String): Int {
        val (hours, minutes) = time.split(":").map { it.toInt() }
        return hours * 60 + minutes
    }

    /** Booking Experience Refactor, spec section 12: "Today, Next six days. Always a rolling 7-day calendar." Exactly 7 entries. */
    val availableDates: List<Pair<String, String>> = listOf(
        "today" to "امروز، ۲۳ تیر",
        "tomorrow" to "فردا، ۲۴ تیر",
        "day_after" to "دوشنبه، ۲۵ تیر",
        "day3" to "سه‌شنبه، ۲۶ تیر",
        "day4" to "چهارشنبه، ۲۷ تیر",
        "day5" to "پنجشنبه، ۲۸ تیر",
        "day6" to "جمعه، ۲۹ تیر",
    )
}
