package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.BookingApi
import ai.rojan.designlab.data.remote.dto.CreateBookingRequestDto
import ai.rojan.designlab.data.remote.dto.RescheduleBookingRequestDto
import ai.rojan.designlab.data.remote.dto.toDomain
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.domain.repository.Booking
import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.domain.repository.PagedResult

class BookingRepositoryImpl(
    private val bookingApi: BookingApi,
) : BookingRepository {

    override suspend fun createBooking(
        salonId: String,
        serviceId: String,
        specialistId: String,
        startTime: String,
        notes: String?,
        idempotencyKey: String?,
    ): Result<Booking> = safeApiCall {
        bookingApi.createBooking(
            request = CreateBookingRequestDto(
                salonId = salonId,
                serviceId = serviceId,
                specialistId = specialistId,
                startTime = startTime,
                notes = notes,
            ),
            idempotencyKey = idempotencyKey,
        )
    }.map { it.toDomain() }

    override suspend fun myBookings(page: Int, size: Int, status: BookingStatus?): Result<PagedResult<Booking>> =
        safeApiCall { bookingApi.myBookings(page = page, size = size, status = status?.name) }
            .map { dto ->
                PagedResult(
                    content = dto.content.map { it.toDomain() },
                    page = dto.page,
                    size = dto.size,
                    totalElements = dto.totalElements,
                    totalPages = dto.totalPages,
                )
            }

    override suspend fun getBooking(bookingId: String): Result<Booking> =
        safeApiCall { bookingApi.getBooking(bookingId) }.map { it.toDomain() }

    override suspend fun cancelBooking(bookingId: String): Result<Booking> =
        safeApiCall { bookingApi.cancelBooking(bookingId) }.map { it.toDomain() }

    override suspend fun confirmBooking(bookingId: String): Result<Booking> =
        safeApiCall { bookingApi.confirmBooking(bookingId) }.map { it.toDomain() }

    override suspend fun completeBooking(bookingId: String): Result<Booking> =
        safeApiCall { bookingApi.completeBooking(bookingId) }.map { it.toDomain() }

    override suspend fun rescheduleBooking(bookingId: String, newStartTime: String): Result<Booking> =
        safeApiCall {
            bookingApi.rescheduleBooking(bookingId, RescheduleBookingRequestDto(newStartTime = newStartTime))
        }.map { it.toDomain() }
}
