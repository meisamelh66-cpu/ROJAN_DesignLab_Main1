package ai.rojan.designlab.ui.money

import kotlin.math.roundToLong

/**
 * FIX-005 (Money Type Safety & Financial Data Hardening).
 *
 * The single, shared money path for this app. Before FIX-005 the Manager
 * side had `manager.data.formatTomanPrice` while the Customer side
 * interpolated raw numbers (`"${x} تومان"` — Western digits, no
 * grouping), and prices crossed `Double` → integer via a mix of
 * `toInt()` (truncating), `roundToInt()` and `roundToLong()`.
 *
 * This does NOT change any data contract: the wire type of
 * `ServiceResponseDto.price` stays `Double`, the domain `Service.price`
 * stays `Double`. It only makes the *client-internal* rounding and the
 * *display* consistent. No currency other than Toman exists in this app;
 * these helpers never convert between currencies.
 */

private val persianDigitChars = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

private fun String.toPersianDigits(): String =
    map { c -> if (c in '0'..'9') persianDigitChars[c - '0'] else c }.joinToString("")

/**
 * The one rounding rule for turning a fractional currency amount into a
 * whole Toman value: round half away from zero (`kotlin.math.roundToLong`
 * semantics — ties toward positive infinity), matching the `roundToInt()`
 * / `roundToLong()` already used at most price sites and replacing the
 * `toInt()` truncation used at a few. Same currency in and out.
 */
fun Double.toTomanLong(): Long = this.roundToLong()

/**
 * The one money display path: grouped digits, Persian numerals, the
 * Persian thousands separator "٬", and the " تومان" suffix.
 * e.g. `1_200_000L` -> "۱٬۲۰۰٬۰۰۰ تومان".
 *
 * Output is byte-identical to the pre-FIX-005 `manager.data.formatTomanPrice`,
 * which now delegates here — so existing Manager screens are unchanged.
 */
fun formatToman(amount: Long): String =
    "%,d".format(amount).toPersianDigits().replace(",", "٬") + " تومان"

/** [Int] convenience for the wallet / cashback / coupon amounts that are already whole Toman. */
fun formatToman(amount: Int): String = formatToman(amount.toLong())
