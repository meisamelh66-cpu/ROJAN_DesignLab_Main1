package ai.rojan.designlab.manager.data

import ai.rojan.designlab.data.remote.ManagerBookingApi
import ai.rojan.designlab.data.remote.dto.BookingResponseDto
import ai.rojan.designlab.data.remote.dto.CreateBookingForCustomerRequestDto
import ai.rojan.designlab.data.remote.dto.NetworkBookingStatus
import ai.rojan.designlab.data.remote.dto.PagedResponseDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TASK-003 (Walk-in Customer Booking): [BackendAppointmentRepository] is the Manager app's own
 * salon-wide booking view - the real-world path through which a walk-in booking (backend
 * `customerId: null`, `crmCustomerId` set) first reaches an Android client. Confirms the private
 * `BookingResponseDto.toDomain(): Appointment` mapping neither crashes nor loses the walk-in's
 * identity (falls back to `crmCustomerId`, mirrored into `Appointment.crmCustomerId`).
 */
class BackendAppointmentRepositoryTest {

    private fun walkInResponse(id: String = "booking-1") = BookingResponseDto(
        id = id,
        salonId = "salon-1",
        serviceId = "service-1",
        specialistId = "specialist-1",
        customerId = null,
        crmCustomerId = "crm-customer-1",
        startTime = "2026-09-20T10:00:00",
        endTime = "2026-09-20T10:30:00",
        status = NetworkBookingStatus.PENDING,
        notes = "Walk-in",
        createdAt = "2026-09-01T00:00:00Z",
        updatedAt = "2026-09-01T00:00:00Z",
    )

    @Test
    fun `sync maps a walk-in booking without crashing - customerId falls back to crmCustomerId`() = runBlocking {
        val api = FakeManagerBookingApi(
            listResult = { PagedResponseDto(content = listOf(walkInResponse()), page = 0, size = 100, totalElements = 1, totalPages = 1) },
        )
        val repository = BackendAppointmentRepository(api, salonId = "salon-1")

        val result = repository.sync()

        assertTrue(result.isSuccess)
        val appointment = repository.getAll().single()
        assertEquals("crm-customer-1", appointment.customerId)
        assertEquals("crm-customer-1", appointment.crmCustomerId)
    }

    @Test
    fun `createForCustomer maps a walk-in booking response without crashing`() = runBlocking {
        val api = FakeManagerBookingApi(createForCustomerResult = { walkInResponse(id = "booking-2") })
        val repository = BackendAppointmentRepository(api, salonId = "salon-1")

        val result = repository.createForCustomer(
            customerId = "crm-customer-1",
            serviceId = "service-1",
            specialistId = "specialist-1",
            startTime = "2026-09-20T10:00:00",
            notes = "Walk-in",
        )

        assertTrue(result.isSuccess)
        assertEquals("crm-customer-1", result.getOrNull()?.customerId)
        assertEquals("crm-customer-1", result.getOrNull()?.crmCustomerId)
    }

    private class FakeManagerBookingApi(
        private val listResult: () -> PagedResponseDto<BookingResponseDto> = { error("list not used by this test") },
        private val createForCustomerResult: (CreateBookingForCustomerRequestDto) -> BookingResponseDto = { error("createForCustomer not used by this test") },
    ) : ManagerBookingApi {
        override suspend fun list(salonId: String, page: Int, size: Int, status: String?): PagedResponseDto<BookingResponseDto> = listResult()
        override suspend fun createForCustomer(salonId: String, request: CreateBookingForCustomerRequestDto): BookingResponseDto = createForCustomerResult(request)
        override suspend fun confirm(bookingId: String): BookingResponseDto = error("confirm not used by this test")
        override suspend fun complete(bookingId: String): BookingResponseDto = error("complete not used by this test")
    }
}
