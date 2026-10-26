package ai.rojan.designlab.data.remote.dto

import ai.rojan.designlab.domain.repository.BookingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * TASK-003 (Walk-in Customer Booking). [BookingResponseDto.toDomain] is the one shared
 * DTO -> domain mapping used by the self-service [ai.rojan.designlab.data.repository.BookingRepositoryImpl]
 * (Customer, and Manager/owner-side via `salonBookings`) and Reception's own
 * `BackendReceptionBookingRepository` alike - a single, direct test here covers both.
 */
class BookingResponseMapperTest {

    private fun response(customerId: String?, crmCustomerId: String?) = BookingResponseDto(
        id = "booking-1",
        salonId = "salon-1",
        serviceId = "service-1",
        specialistId = "specialist-1",
        customerId = customerId,
        crmCustomerId = crmCustomerId,
        startTime = "2026-09-20T10:00:00",
        endTime = "2026-09-20T10:30:00",
        status = NetworkBookingStatus.PENDING,
        notes = null,
        createdAt = "2026-09-01T00:00:00Z",
        updatedAt = "2026-09-01T00:00:00Z",
    )

    @Test
    fun `a linked booking maps customerId straight through, with crmCustomerId null`() {
        val booking = response(customerId = "user-1", crmCustomerId = null).toDomain()

        assertEquals("user-1", booking.customerId)
        assertNull(booking.crmCustomerId)
        assertEquals(BookingStatus.PENDING, booking.status)
    }

    @Test
    fun `a walk-in booking (null customerId) falls back to crmCustomerId, never crashing or leaving customerId blank`() {
        val booking = response(customerId = null, crmCustomerId = "crm-customer-1").toDomain()

        assertEquals("crm-customer-1", booking.customerId)
        assertEquals("crm-customer-1", booking.crmCustomerId)
    }
}
