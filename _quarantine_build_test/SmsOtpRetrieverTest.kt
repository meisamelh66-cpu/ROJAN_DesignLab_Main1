package ai.rojan.designlab.manager.screens.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** [extractOtpCode] mirrors ROJAN_Backend's `RequestOtpUseCase.buildMessage` message shape - kept in sync manually, same as `MeliPayamakSharedPatternProvider.CODE_PATTERN` on the backend side. */
class SmsOtpRetrieverTest {

    @Test
    fun `extracts the 4-digit code from a plain (non-SMS-Retriever-framed) message`() {
        val sms = "Your ROJAN verification code is 4821. It expires in 2 minutes.\n@rojanai.ir #4821"
        assertEquals("4821", extractOtpCode(sms))
    }

    @Test
    fun `extracts the 4-digit code from an SMS-Retriever-framed message with the app hash line`() {
        val sms = "<#> Your ROJAN verification code is 4821. It expires in 2 minutes.\n8mclTwWJEbF\n@rojanai.ir #4821"
        assertEquals("4821", extractOtpCode(sms))
    }

    @Test
    fun `returns null for a malformed message with no matching code`() {
        val sms = "<#> Your account was updated.\n8mclTwWJEbF"
        assertNull(extractOtpCode(sms))
    }

    @Test
    fun `does not match a 5-plus digit run - only a 4-digit ROJAN OTP is ever expected`() {
        val sms = "Your ROJAN verification code is 48213. It expires in 2 minutes."
        assertNull(extractOtpCode(sms))
    }
}
