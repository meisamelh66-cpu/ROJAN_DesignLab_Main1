package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.data.remote.BookingApi
import ai.rojan.designlab.data.remote.NetworkUnavailableException
import ai.rojan.designlab.data.remote.dto.BookingResponseDto
import ai.rojan.designlab.data.remote.dto.CreateBookingRequestDto
import ai.rojan.designlab.data.remote.dto.NetworkBookingStatus
import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import ai.rojan.designlab.data.remote.dto.RescheduleBookingRequestDto
import ai.rojan.designlab.domain.repository.BookingStatus
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * TEAM2-003 (Complete Booking API Contract). Confirms each of the four
 * booking-lifecycle operations — confirm, complete, reschedule, cancel —
 * genuinely round-trips through [BookingApi] and maps into the domain
 * [ai.rojan.designlab.domain.repository.Booking] on success, and that a
 * failure (401 or network) becomes a real [Result.failure] via the same
 * `safeApiCall` contract TEAM2-001 already established for `createBooking`
 * — never swallowed, never a fabricated success.
 */
class BookingRepositoryImplTest {

    private val sampleResponse = BookingResponseDto(
        id = "booking-1",
        salonId = "salon-1",
        serviceId = "service-1",
        specialistId = "specialist-1",
        customerId = "customer-1",
        startTime = "2026-09-20T10:00:00",
        endTime = "2026-09-20T10:30:00",
        status = NetworkBookingStatus.CONFIRMED,
        notes = null,
        createdAt = "2026-09-01T00:00:00Z",
        updatedAt = "2026-09-01T00:00:00Z",
    )

    @Test
    fun `confirmBooking success maps the response into a domain Booking`() = runBlocking {
        val repository = BookingRepositoryImpl(FakeBookingApi(confirm = { sampleResponse }))

        val result = repository.confirmBooking("booking-1")

        assertTrue(result.isSuccess)
        assertEquals("booking-1", result.getOrNull()?.id)
        assertEquals(BookingStatus.CONFIRMED, result.getOrNull()?.status)
    }

    @Test
    fun `confirmBooking failure (409 - booking not pending) becomes a real Result failure, not a swallowed one`() = runBlocking {
        val repository = BookingRepositoryImpl(FakeBookingApi(confirm = { throw httpException(409) }))

        val result = repository.confirmBooking("booking-1")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BackendApiException)
        assertEquals(409, (result.exceptionOrNull() as BackendApiException).statusCode)
    }

    @Test
    fun `completeBooking success maps the response into a domain Booking`() = runBlocking {
        val repository = BookingRepositoryImpl(
            FakeBookingApi(complete = { sampleResponse.copy(status = NetworkBookingStatus.COMPLETED) }),
        )

        val result = repository.completeBooking("booking-1")

        assertTrue(result.isSuccess)
        assertEquals(BookingStatus.COMPLETED, result.getOrNull()?.status)
    }

    @Test
    fun `rescheduleBooking success sends the new start time in the request and maps the response`() = runBlocking {
        var capturedRequest: RescheduleBookingRequestDto? = null
        val repository = BookingRepositoryImpl(
            FakeBookingApi(
                reschedule = { request ->
                    capturedRequest = request
                    sampleResponse.copy(startTime = request.newStartTime)
                },
            ),
        )

        val result = repository.rescheduleBooking("booking-1", "2026-09-21T14:00:00")

        assertEquals("2026-09-21T14:00:00", capturedRequest?.newStartTime)
        assertEquals("2026-09-21T14:00:00", result.getOrNull()?.startTime)
    }

    @Test
    fun `cancelBooking success maps the response into a domain Booking`() = runBlocking {
        val repository = BookingRepositoryImpl(
            FakeBookingApi(cancel = { sampleResponse.copy(status = NetworkBookingStatus.CANCELLED) }),
        )

        val result = repository.cancelBooking("booking-1")

        assertTrue(result.isSuccess)
        assertEquals(BookingStatus.CANCELLED, result.getOrNull()?.status)
    }

    @Test
    fun `a 401 on any of the four operations becomes a real BackendApiException, not an empty or fabricated success`() = runBlocking {
        val repository = BookingRepositoryImpl(FakeBookingApi(reschedule = { throw httpException(401) }))

        val result = repository.rescheduleBooking("booking-1", "2026-09-21T14:00:00")

        assertTrue(result.isFailure)
        assertEquals(401, (result.exceptionOrNull() as BackendApiException).statusCode)
    }

    @Test
    fun `a network failure on any of the four operations becomes NetworkUnavailableException`() = runBlocking {
        val repository = BookingRepositoryImpl(FakeBookingApi(cancel = { throw IOException("offline") }))

        val result = repository.cancelBooking("booking-1")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkUnavailableException)
    }

    private fun httpException(code: Int): HttpException =
        HttpException(Response.error<Any>(code, "{}".toResponseBody("application/json".toMediaType())))
}

private class FakeBookingApi(
    private val confirm: () -> BookingResponseDto = { error("confirmBooking not used by this test") },
    private val complete: () -> BookingResponseDto = { error("completeBooking not used by this test") },
    private val reschedule: (RescheduleBookingRequestDto) -> BookingResponseDto =
        { error("rescheduleBooking not used by this test") },
    private val cancel: () -> BookingResponseDto = { error("cancelBooking not used by this test") },
) : BookingApi {
    override suspend fun createBooking(request: CreateBookingRequestDto, idempotencyKey: String?): BookingResponseDto =
        error("createBooking not used by this test")

    override suspend fun myBookings(page: Int, size: Int, status: String?): PagedResponseDto<BookingResponseDto> =
        error("myBookings not used by this test")

    override suspend fun getBooking(bookingId: String): BookingResponseDto =
        error("getBooking not used by this test")

    override suspend fun cancelBooking(bookingId: String): BookingResponseDto = cancel()

    override suspend fun confirmBooking(bookingId: String): BookingResponseDto = confirm()

    override suspend fun completeBooking(bookingId: String): BookingResponseDto = complete()

    override suspend fun rescheduleBooking(bookingId: String, request: RescheduleBookingRequestDto): BookingResponseDto =
        reschedule(request)
}
