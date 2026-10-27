package ai.rojan.designlab.manager.data

import ai.rojan.designlab.data.remote.ManagerBookingApi
import ai.rojan.designlab.data.remote.dto.BookingResponseDto
import ai.rojan.designlab.data.remote.dto.CreateBookingForCustomerRequestDto
import ai.rojan.designlab.data.remote.dto.NetworkBookingStatus
import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Final Blocker Closure Sprint — the first test coverage for
 * [BackendAppointmentRepository.cancel], which moved from local-cache-only
 * (silently overwritten on the next [BackendAppointmentRepository.sync],
 * never actually persisted) to a real `PATCH /api/v1/bookings/{id}/cancel`
 * call, following [BackendAppointmentRepository.confirm]'s exact pattern.
 * Scoped to [BackendAppointmentRepository.cancel] only - not a retroactive
 * audit of this repository's other methods.
 */
class BackendAppointmentRepositoryTest {

    private val salonId = "salon-1"
    private val bookingId = "booking-1"

    private fun bookingDto(status: NetworkBookingStatus) = BookingResponseDto(
        id = bookingId,
        salonId = salonId,
        serviceId = "service-1",
        specialistId = "specialist-1",
        customerId = "customer-1",
        startTime = "2026-09-01T10:00:00",
        endTime = "2026-09-01T10:30:00",
        status = status,
        notes = null,
        createdAt = "2026-08-01T10:00:00",
        updatedAt = "2026-08-01T10:00:00",
    )

    @Test
    fun `cancel success calls the real backend endpoint and updates the cache to CANCELLED`() = runBlocking {
        val api = FakeManagerBookingApi(
            listResult = { PagedResponseDto(content = listOf(bookingDto(NetworkBookingStatus.PENDING)), totalElements = 1L, totalPages = 1, page = 0, size = 100) },
            cancelResult = { id -> assertEquals(bookingId, id); bookingDto(NetworkBookingStatus.CANCELLED) },
        )
        val repository = BackendAppointmentRepository(api, salonId)
        repository.sync()
        assertEquals(AppointmentStatus.PENDING, repository.getById(bookingId)?.status)

        val result = repository.cancel(bookingId)

        assertTrue(result.isSuccess)
        assertEquals(AppointmentStatus.CANCELLED, result.getOrNull()?.status)
        // the in-memory cache getAll()/getById() read from must reflect the same change - not
        // require a separate sync() to see it.
        assertEquals(AppointmentStatus.CANCELLED, repository.getById(bookingId)?.status)
    }

    @Test
    fun `cancel backend failure returns a failed Result and leaves the cache unchanged - never a false success`() = runBlocking {
        val api = FakeManagerBookingApi(
            listResult = { PagedResponseDto(content = listOf(bookingDto(NetworkBookingStatus.PENDING)), totalElements = 1L, totalPages = 1, page = 0, size = 100) },
            cancelResult = { throw IOException("network down") },
        )
        val repository = BackendAppointmentRepository(api, salonId)
        repository.sync()

        val result = repository.cancel(bookingId)

        assertTrue(result.isFailure)
        // a rejected cancel must never be shown as cancelled - the cached status stays exactly
        // what the last successful sync/mutation said it was.
        assertEquals(AppointmentStatus.PENDING, repository.getById(bookingId)?.status)
    }

    @Test
    fun `a fresh sync after a successful cancel still reflects CANCELLED - refresh consistency`() = runBlocking {
        var backendStatus = NetworkBookingStatus.PENDING
        val api = FakeManagerBookingApi(
            listResult = { PagedResponseDto(content = listOf(bookingDto(backendStatus)), totalElements = 1L, totalPages = 1, page = 0, size = 100) },
            cancelResult = { backendStatus = NetworkBookingStatus.CANCELLED; bookingDto(NetworkBookingStatus.CANCELLED) },
        )
        val repository = BackendAppointmentRepository(api, salonId)
        repository.sync()

        repository.cancel(bookingId)
        repository.sync() // simulates a later reload/refresh (e.g. navigating back to the list)

        assertEquals(AppointmentStatus.CANCELLED, repository.getById(bookingId)?.status)
    }

    private class FakeManagerBookingApi(
        private val listResult: suspend () -> PagedResponseDto<BookingResponseDto> = { error("not used by these tests") },
        private val cancelResult: suspend (String) -> BookingResponseDto = { error("not used by these tests") },
    ) : ManagerBookingApi {
        override suspend fun list(salonId: String, page: Int, size: Int, status: String?): PagedResponseDto<BookingResponseDto> = listResult()
        override suspend fun createForCustomer(salonId: String, request: CreateBookingForCustomerRequestDto): BookingResponseDto = error("not used by these tests")
        override suspend fun confirm(bookingId: String): BookingResponseDto = error("not used by these tests")
        override suspend fun complete(bookingId: String): BookingResponseDto = error("not used by these tests")
        override suspend fun cancel(bookingId: String): BookingResponseDto = cancelResult(bookingId)
    }
}
