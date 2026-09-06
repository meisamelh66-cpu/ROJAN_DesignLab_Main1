package ai.rojan.designlab.ui.money

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * FIX-005. The shared money path: one rounding rule, one display format.
 */
class TomanTest {

    @Test
    fun `toTomanLong rounds half up, never truncates`() {
        // The pre-FIX-005 bug: `.toInt()` at some price sites truncated.
        assertEquals(450_000L, 449_999.6.toTomanLong())
        assertEquals(450_001L, 450_000.5.toTomanLong())
        assertEquals(450_000L, 450_000.4.toTomanLong())
        // Whole values are unchanged (no unintended conversion).
        assertEquals(1_200_000L, 1_200_000.0.toTomanLong())
        assertEquals(0L, 0.0.toTomanLong())
    }

    @Test
    fun `formatToman groups digits, uses Persian numerals and the Toman suffix`() {
        assertEquals("۱٬۲۰۰٬۰۰۰ تومان", formatToman(1_200_000L))
        assertEquals("۴۵۰٬۰۰۰ تومان", formatToman(450_000))
        assertEquals("۰ تومان", formatToman(0L))
    }

    @Test
    fun `formatToman output is unchanged from the previous Manager-only formatter`() {
        // Byte-for-byte match with the old `manager.data.formatTomanPrice`
        // implementation, so existing Manager screens don't shift.
        val legacy = "%,d".format(2_500_000L)
            .map { c -> if (c in '0'..'9') "۰۱۲۳۴۵۶۷۸۹"[c - '0'] else c }
            .joinToString("")
            .replace(",", "٬") + " تومان"
        assertEquals(legacy, formatToman(2_500_000L))
    }
}
