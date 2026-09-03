package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.BookingApi
import ai.rojan.designlab.data.remote.dto.BookingResponseDto
import ai.rojan.designlab.data.remote.dto.CreateBookingRequestDto
import ai.rojan.designlab.data.remote.dto.NetworkBookingStatus
import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import ai.rojan.designlab.data.remote.dto.RescheduleBookingRequestDto
import ai.rojan.designlab.domain.repository.BookingStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Reliability Sprint 01, P0-3: hermetic coverage of [BookingRepositoryImpl]'s
 * create/cancel/reschedule mutations against a fake [BookingApi] — same
 * shape [CustomerRelationshipRepositoryImplTest] already establishes for a
 * repository-implementation test (fake the Retrofit interface directly,
 * not the repository).
 */
private class FakeBookingApi : BookingApi {
    var createResponse: BookingResponseDto? = null
    var cancelResponse: BookingResponseDto? = null
    var rescheduleResponse: BookingResponseDto? = null
    var thrownOnCreate: Throwable? = null
    var thrownOnCancel: Throwable? = null
    var thrownOnReschedule: Throwable? = null

    var lastCreateRequest: CreateBookingRequestDto? = null
    var lastIdempotencyKey: String? = null
    var lastRescheduleRequest: RescheduleBookingRequestDto? = null

    override suspend fun createBooking(request: CreateBookingRequestDto, idempotencyKey: String?): BookingResponseDto {
        thrownOnCreate?.let { throw it }
        lastCreateRequest = request
        lastIdempotencyKey = idempotencyKey
        return createResponse!!
    }

    override suspend fun myBookings(page: Int, size: Int, status: String?): PagedResponseDto<BookingResponseDto> =
        error("not used")

    override suspend fun getBooking(bookingId: String): BookingResponseDto = error("not used")

    override suspend fun cancelBooking(bookingId: String): BookingResponseDto {
        thrownOnCancel?.let { throw it }
        return cancelResponse!!
    }

    override suspend fun confirmBooking(bookingId: String): BookingResponseDto = error("not used")

    override suspend fun completeBooking(bookingId: String): BookingResponseDto = error("not used")

    override suspend fun rescheduleBooking(bookingId: String, request: RescheduleBookingRequestDto): BookingResponseDto {
        thrownOnReschedule?.let { throw it }
        lastRescheduleRequest = request
        return rescheduleResponse!!
    }
}

private fun sampleBookingResponse(id: String = "b1", status: NetworkBookingStatus = NetworkBookingStatus.PENDING) = BookingResponseDto(
    id = id,
    salonId = "s1",
    serviceId = "sv1",
    specialistId = "sp1",
    customerId = "c1",
    startTime = "2026-09-01T10:00:00",
    endTime = "2026-09-01T10:30:00",
    status = status,
    notes = null,
    createdAt = "2026-09-01T09:00:00Z",
    updatedAt = "2026-09-01T09:00:00Z",
)

class BookingRepositoryImplTest {

    private val api = FakeBookingApi()
    private val repository = BookingRepositoryImpl(api)

    @Test
    fun `createBooking maps a successful backend response to the domain Booking`() = runTest {
        api.createResponse = sampleBookingResponse(id = "b1", status = NetworkBookingStatus.PENDING)

        val result = repository.createBooking(
            salonId = "s1",
            serviceId = "sv1",
            specialistId = "sp1",
            startTime = "2026-09-01T10:00:00",
            notes = null,
            idempotencyKey = "key-1",
        )

        assertTrue(result.isSuccess)
        val booking = result.getOrThrow()
        assertEquals("b1", booking.id)
        assertEquals(BookingStatus.PENDING, booking.status)
        assertEquals("key-1", api.lastIdempotencyKey)
        assertEquals("sv1", api.lastCreateRequest?.serviceId)
    }

    @Test
    fun `createBooking propagates a backend failure as Result failure`() = runTest {
        api.thrownOnCreate = IOException("network down")

        val result = repository.createBooking(
            salonId = "s1",
            serviceId = "sv1",
            specialistId = "sp1",
            startTime = "2026-09-01T10:00:00",
            notes = null,
            idempotencyKey = null,
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `cancelBooking maps a successful backend response to the domain Booking`() = runTest {
        api.cancelResponse = sampleBookingResponse(id = "b1", status = NetworkBookingStatus.CANCELLED)

        val result = repository.cancelBooking("b1")

        assertTrue(result.isSuccess)
        assertEquals(BookingStatus.CANCELLED, result.getOrThrow().status)
    }

    @Test
    fun `cancelBooking propagates a backend failure as Result failure`() = runTest {
        api.thrownOnCancel = IOException("boom")

        val result = repository.cancelBooking("b1")

        assertTrue(result.isFailure)
    }

    @Test
    fun `rescheduleBooking maps a successful backend response to the domain Booking`() = runTest {
        api.rescheduleResponse = sampleBookingResponse(id = "b1", status = NetworkBookingStatus.CONFIRMED)

        val result = repository.rescheduleBooking("b1", "2026-09-02T14:00:00")

        assertTrue(result.isSuccess)
        assertEquals(BookingStatus.CONFIRMED, result.getOrThrow().status)
        assertEquals("2026-09-02T14:00:00", api.lastRescheduleRequest?.newStartTime)
    }

    @Test
    fun `rescheduleBooking propagates a backend failure as Result failure`() = runTest {
        api.thrownOnReschedule = IOException("boom")

        val result = repository.rescheduleBooking("b1", "2026-09-02T14:00:00")

        assertTrue(result.isFailure)
    }
}
