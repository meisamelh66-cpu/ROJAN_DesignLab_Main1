package ai.rojan.designlab.manager.screens.settings

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Real-device RTL/UX fix — covers [formatTimeInput]/[isCompleteTime], the
 * pure HH:mm digit-mask functions [TimeField] uses instead of letting the
 * manager type a colon manually or letting an out-of-range hour/minute
 * through.
 *
 * Real-device follow-up (cursor-jumping fix): also covers [applyTimeMask],
 * which pairs the masked text with an explicit cursor position so manual
 * editing (deleting, replacing a digit, retyping) doesn't fight the
 * cursor the way a plain `onValueChange = { formatTimeInput(it) }` did.
 */
class ManagerWorkingHoursScreenTest {

    @Test
    fun `typing a single-digit hour plus two minute digits auto-pads and inserts the colon`() {
        assertEquals("09:00", formatTimeInput("900"))
        assertEquals("09:30", formatTimeInput("930"))
    }

    @Test
    fun `typing a two-digit hour plus two minute digits inserts the colon`() {
        assertEquals("15:30", formatTimeInput("1530"))
        assertEquals("18:00", formatTimeInput("1800"))
        assertEquals("23:45", formatTimeInput("2345"))
    }

    @Test
    fun `partial input with fewer than three digits has no colon yet`() {
        assertEquals("", formatTimeInput(""))
        assertEquals("9", formatTimeInput("9"))
        assertEquals("15", formatTimeInput("15"))
    }

    @Test
    fun `an out-of-range hour is clamped to 23`() {
        assertEquals("23:00", formatTimeInput("9900"))
    }

    @Test
    fun `an out-of-range minute is clamped to 59`() {
        assertEquals("09:59", formatTimeInput("999"))
        assertEquals("15:59", formatTimeInput("1599"))
    }

    @Test
    fun `non-digit characters are stripped - the colon can never be typed manually`() {
        assertEquals("15:30", formatTimeInput("15:30"))
        assertEquals("15:30", formatTimeInput("1a5b3c0"))
    }

    @Test
    fun `a fifth digit is dropped - input never exceeds HH mm`() {
        assertEquals("15:30", formatTimeInput("15309"))
    }

    @Test
    fun `isCompleteTime only accepts a fully-formed HH mm value`() {
        assertTrue("09:00".isCompleteTime())
        assertTrue("23:59".isCompleteTime())
        assertTrue(!"9".isCompleteTime())
        assertTrue(!"15".isCompleteTime())
        assertTrue(!"".isCompleteTime())
    }

    @Test
    fun `isCompleteTime also accepts a backend-sourced HH mm ss value unchanged by the manager`() {
        // WorkingHoursDtos.kt's own doc comment: the backend returns e.g.
        // "09:00:00" - an already-saved, never-edited day must not show its
        // Save button as disabled just because it carries seconds.
        assertTrue("09:00:00".isCompleteTime())
        assertTrue("23:59:59".isCompleteTime())
    }

    @Test
    fun `displayTimeValue truncates a backend HH mm ss value for display only`() {
        assertEquals("09:00", displayTimeValue("09:00:00"))
        assertEquals("15:30", displayTimeValue("15:30:45"))
    }

    @Test
    fun `displayTimeValue leaves an already-HH-mm or in-progress value untouched`() {
        assertEquals("09:00", displayTimeValue("09:00"))
        assertEquals("15", displayTimeValue("15"))
        assertEquals("", displayTimeValue(""))
    }

    private fun typed(text: String, cursor: Int) = TextFieldValue(text = text, selection = TextRange(cursor))

    @Test
    fun `typing digits one at a time keeps the cursor at the end as the colon appears`() {
        var result = applyTimeMask(typed("9", 1))
        assertEquals("9", result.text)
        assertEquals(TextRange(1), result.selection)

        result = applyTimeMask(typed("93", 2))
        assertEquals("93", result.text)
        assertEquals(TextRange(2), result.selection)

        // The 3rd digit triggers the single-digit-hour pad ("9" -> "09") -
        // the cursor must still land at the end, not one digit short of it.
        result = applyTimeMask(typed("930", 3))
        assertEquals("09:30", result.text)
        assertEquals(TextRange(5), result.selection)
    }

    @Test
    fun `typing a full two-digit hour and minute keeps the cursor at the end`() {
        val result = applyTimeMask(typed("1530", 4))
        assertEquals("15:30", result.text)
        assertEquals(TextRange(5), result.selection)
    }

    @Test
    fun `replacing a single digit mid-value keeps the cursor right after the replacement - 15-30 to 16-30`() {
        // User selected the "5" in "15:30" and typed "6": Android's own text
        // editing already produced this exact TextFieldValue before our
        // mask runs.
        val result = applyTimeMask(typed("16:30", 2))
        assertEquals("16:30", result.text)
        assertEquals(TextRange(2), result.selection)
    }

    @Test
    fun `replacing the whole value keeps the cursor at the end - 15-30 to 09-45`() {
        val result = applyTimeMask(typed("0945", 4))
        assertEquals("09:45", result.text)
        assertEquals(TextRange(5), result.selection)
    }

    @Test
    fun `deleting the trailing digit does not throw and keeps a valid, cursor-stable result`() {
        // Backspacing from a complete "09:30" removes the trailing '0'.
        val result = applyTimeMask(typed("09:3", 4))
        assertEquals(5, result.text.length)
        assertTrue(result.text.matches(Regex("""\d{2}:\d{2}""")))
        assertEquals(TextRange(result.text.length), result.selection)
    }

    @Test
    fun `clearing the field entirely returns an empty value with the cursor at the start`() {
        val result = applyTimeMask(typed("", 0))
        assertEquals("", result.text)
        assertEquals(TextRange(0), result.selection)
    }

    @Test
    fun `editing in the middle of a two-digit hour keeps the cursor just after the edited digit`() {
        // Cursor was between the two hour digits ("1|5:30") when the user
        // typed something that replaced the "1".
        val result = applyTimeMask(typed("25:30", 1))
        assertEquals("23:30", result.text) // hour 25 clamped to 23
        assertEquals(TextRange(1), result.selection)
    }
}
