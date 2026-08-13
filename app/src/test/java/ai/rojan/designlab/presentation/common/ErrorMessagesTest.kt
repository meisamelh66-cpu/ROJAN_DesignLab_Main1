package ai.rojan.designlab.presentation.common

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.data.remote.NetworkUnavailableException
import ai.rojan.designlab.data.remote.RequestTimeoutException
import ai.rojan.designlab.data.remote.dto.ApiErrorDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.io.IOException

/**
 * System2 Android Parallel Work, Phase B — coverage for the four
 * distinct error categories `userMessageFor` is now required to separate
 * (Phase A item 2): `400` validation, network failures, timeout errors,
 * and server (`5xx`) errors. The core requirement under test is that a
 * `400` never produces the same message as a network failure — the
 * literal "validation errors must not display as network errors" rule.
 */
class ErrorMessagesTest {

    private fun apiException(status: Int, message: String = "irrelevant") = BackendApiException(
        statusCode = status,
        apiError = ApiErrorDto(
            timestamp = "2026-08-13T00:00:00Z",
            status = status,
            error = "Error",
            errorCode = null,
            message = message,
            path = "/api/v1/test",
        ),
    )

    @Test
    fun `a 400 validation error is distinct from the network-unavailable message`() {
        val validationMessage = userMessageFor(apiException(400))
        val networkMessage = userMessageFor(NetworkUnavailableException(IOException("no route to host")))

        assertNotEquals("a 400 must not display as a network error", networkMessage, validationMessage)
    }

    @Test
    fun `a 400 validation error is distinct from a timeout message`() {
        val validationMessage = userMessageFor(apiException(400))
        val timeoutMessage = userMessageFor(RequestTimeoutException(IOException("timed out")))

        assertNotEquals(timeoutMessage, validationMessage)
    }

    @Test
    fun `a 400 validation error never leaks the raw backend message`() {
        val message = userMessageFor(apiException(400, message = "phoneNumber is not valid E.164"))

        assertNotEquals("phoneNumber is not valid E.164", message)
    }

    @Test
    fun `a timeout is distinct from a plain network failure`() {
        val timeoutMessage = userMessageFor(RequestTimeoutException(IOException("timed out")))
        val networkMessage = userMessageFor(NetworkUnavailableException(IOException("no route to host")))

        assertNotEquals(timeoutMessage, networkMessage)
    }

    @Test
    fun `a 5xx server error is distinct from both network and validation messages`() {
        val serverMessage = userMessageFor(apiException(500))
        val networkMessage = userMessageFor(NetworkUnavailableException(IOException("offline")))
        val validationMessage = userMessageFor(apiException(400))

        assertNotEquals(serverMessage, networkMessage)
        assertNotEquals(serverMessage, validationMessage)
    }

    @Test
    fun `every known status code and failure kind maps to a non-blank message`() {
        val cases = listOf(
            userMessageFor(apiException(400)),
            userMessageFor(apiException(401)),
            userMessageFor(apiException(403)),
            userMessageFor(apiException(404)),
            userMessageFor(apiException(409)),
            userMessageFor(apiException(500)),
            userMessageFor(apiException(503)),
            userMessageFor(NetworkUnavailableException(IOException())),
            userMessageFor(RequestTimeoutException(IOException())),
            userMessageFor(IllegalStateException("unexpected")),
        )

        cases.forEach { assertNotEquals("", it) }
    }

    @Test
    fun `repeated calls for the same category are stable`() {
        assertEquals(userMessageFor(apiException(400)), userMessageFor(apiException(400, message = "a different raw message")))
    }
}
