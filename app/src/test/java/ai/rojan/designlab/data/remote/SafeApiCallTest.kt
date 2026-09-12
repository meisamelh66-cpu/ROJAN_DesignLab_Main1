package ai.rojan.designlab.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

/**
 * Engineering Cleanup Phase 4 (P2): a malformed / contract-drifted backend
 * body used to escape [safeApiCall] as an uncaught [SerializationException]
 * (a `RuntimeException`, not an [IOException]) and crash the process. These
 * lock in that it — and any other unforeseen exception — now comes back as a
 * `Result.failure` with a narrow type, while coroutine cancellation still
 * propagates untouched.
 */
class SafeApiCallTest {

    @Serializable
    private data class Strict(val id: String, val name: String)

    @Test
    fun `success passes the value straight through`() = runTest {
        val result = safeApiCall { 42 }
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `a SerializationException becomes MalformedResponseException, not a throw`() = runTest {
        val result = safeApiCall<Strict> {
            // exactly what the Retrofit kotlinx-serialization converter does on a
            // body whose shape no longer matches the DTO
            Json.decodeFromString(Strict.serializer(), """{"id":"s1"}""")
        }
        assertTrue("expected failure", result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue("got $error", error is MalformedResponseException)
        assertTrue("should be an IOException subtype", error is IOException)
        assertTrue(error!!.cause is MissingFieldException)
    }

    @Test
    fun `raw malformed JSON also becomes MalformedResponseException`() = runTest {
        val result = safeApiCall<Strict> {
            Json.decodeFromString(Strict.serializer(), "not json at all")
        }
        assertTrue(result.exceptionOrNull() is MalformedResponseException)
    }

    @Test
    fun `an unforeseen exception becomes UnexpectedApiException`() = runTest {
        val result = safeApiCall<Int> { throw IllegalStateException("boom") }
        val error = result.exceptionOrNull()
        assertTrue("got $error", error is UnexpectedApiException)
        assertTrue(error is IOException)
        assertEquals("boom", error!!.cause?.message)
    }

    @Test
    fun `a plain IOException still becomes NetworkUnavailableException`() = runTest {
        val result = safeApiCall<Int> { throw IOException("no route to host") }
        assertTrue(result.exceptionOrNull() is NetworkUnavailableException)
    }

    @Test
    fun `CancellationException is re-thrown, never swallowed into a Result`() = runTest {
        val marker = CancellationException("cancelled")
        try {
            safeApiCall<Int> { throw marker }
            fail("safeApiCall should have re-thrown the CancellationException")
        } catch (e: CancellationException) {
            assertSame(marker, e)
        }
    }

    @Test
    fun `a bare SerializationException subclass is covered`() = runTest {
        val result = safeApiCall<Int> { throw SerializationException("decode failed") }
        assertNotNull(result.exceptionOrNull())
        assertTrue(result.exceptionOrNull() is MalformedResponseException)
    }
}
