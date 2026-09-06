package ai.rojan.designlab.manager.data

import ai.rojan.designlab.ui.money.formatToman

/**
 * Manager Booking Journey Phase 2 — small formatting helpers for the
 * genuinely new numeric values the booking flow and Dashboard KPIs now
 * compute (service prices, appointment counts, occupancy percentages).
 * Every existing Manager/Customer display string in this codebase is a
 * hand-written Persian-numeral literal; there was no prior digit-
 * conversion utility to reuse, and rendering computed numbers in
 * Western digits would be a real, visible regression against the rest
 * of this RTL Persian UI.
 */
private val persianDigitChars = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

fun String.toPersianDigits(): String =
    map { c -> if (c in '0'..'9') persianDigitChars[c - '0'] else c }.joinToString("")

fun Long.toPersianDigits(): String = toString().toPersianDigits()

fun Int.toPersianDigits(): String = toString().toPersianDigits()

/**
 * e.g. 1_200_000 -> "۱٬۲۰۰٬۰۰۰ تومان".
 *
 * FIX-005: now delegates to the shared [formatToman] so Manager and
 * Customer money render through one path. Output is unchanged.
 */
fun formatTomanPrice(price: Long): String = formatToman(price)

/** e.g. 90 -> "۹۰ دقیقه". */
fun formatDurationMinutes(minutes: Int): String = "${minutes.toPersianDigits()} دقیقه"
