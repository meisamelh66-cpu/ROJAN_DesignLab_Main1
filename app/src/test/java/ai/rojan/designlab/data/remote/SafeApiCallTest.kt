package ai.rojan.designlab.data.remote

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * TEAM2-001 (Booking Transaction Integrity) root-cause coverage: before this
 * fix, a 2xx response whose body failed to decode into the expected DTO
 * (malformed JSON, a required field missing/wrong type — exactly what
 * `retrofit2:converter-kotlinx-serialization` throws as a
 * [SerializationException], a [RuntimeException], not an [IOException])
 * passed straight through [safeApiCall] uncaught instead of becoming a
 * [Result.failure] — crashing the caller rather than reaching an error
 * state. This is the "Invalid response -> Error state" case.
 */
class SafeApiCallTest {

    @Test
    fun `a successful call passes its value through unchanged`() = runBlocking {
        val result = safeApiCall { "ok" }

        assertEquals("ok", result.getOrNull())
    }

    @Test
    fun `a connectivity failure becomes NetworkUnavailableException, not a crash`() = runBlocking {
        val result = safeApiCall<String> { throw IOException("no route to host") }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkUnavailableException)
    }

    @Test
    fun `a response body that fails to decode becomes InvalidResponseException, not an uncaught crash`() = runBlocking {
        val result = safeApiCall<String> {
            // Simulates exactly what the kotlinx-serialization Retrofit
            // converter throws when a 2xx response body doesn't match the
            // expected DTO shape.
            throw SerializationException("Field 'id' is required for type 'BookingResponseDto', but it was missing")
        }

        assertTrue(result.isFailure)
        assertTrue(
            "a decode failure must become a Result.failure the caller can handle, not propagate uncaught",
            result.exceptionOrNull() is InvalidResponseException,
        )
    }
}
