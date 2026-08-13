package ai.rojan.designlab.domain.phone

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * System2 Android Parallel Work, Phase B — coverage for the two rules
 * [normalizeIranianPhoneNumber] is specified to handle, plus the
 * deliberate passthrough behavior for anything else.
 */
class PhoneNumberNormalizerTest {

    @Test
    fun `a local-format number with a leading zero is converted to E164`() {
        assertEquals("+98912xxxxxxx", normalizeIranianPhoneNumber("0912xxxxxxx"))
    }

    @Test
    fun `an already E164 number is returned unchanged`() {
        assertEquals("+98912xxxxxxx", normalizeIranianPhoneNumber("+98912xxxxxxx"))
    }

    @Test
    fun `surrounding whitespace is trimmed before normalization`() {
        assertEquals("+98912xxxxxxx", normalizeIranianPhoneNumber("  0912xxxxxxx  "))
        assertEquals("+98912xxxxxxx", normalizeIranianPhoneNumber("  +98912xxxxxxx  "))
    }

    @Test
    fun `a number with neither a leading zero nor a country code is passed through unchanged, not guessed at`() {
        assertEquals("912xxxxxxx", normalizeIranianPhoneNumber("912xxxxxxx"))
    }

    @Test
    fun `an empty or blank input is passed through unchanged`() {
        assertEquals("", normalizeIranianPhoneNumber(""))
        assertEquals("", normalizeIranianPhoneNumber("   "))
    }

    @Test
    fun `a real-shaped number normalizes correctly, not just the placeholder pattern`() {
        assertEquals("+989123456789", normalizeIranianPhoneNumber("09123456789"))
        assertEquals("+989123456789", normalizeIranianPhoneNumber("+989123456789"))
    }
}
