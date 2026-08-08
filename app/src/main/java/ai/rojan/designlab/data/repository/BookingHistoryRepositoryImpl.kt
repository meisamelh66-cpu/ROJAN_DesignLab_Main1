package ai.rojan.designlab.data.repository

import ai.rojan.designlab.domain.repository.BookingHistoryRepository
import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.domain.repository.BookingWithDetails
import ai.rojan.designlab.domain.repository.PagedResult
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.repository.SpecialistRepository

class BookingHistoryRepositoryImpl(
    private val bookingRepository: BookingRepository,
    private val salonRepository: SalonRepository,
    private val specialistRepository: SpecialistRepository,
) : BookingHistoryRepository {

    override suspend fun myBookingsWithDetails(
        page: Int,
        size: Int,
        status: BookingStatus?,
    ): Result<PagedResult<BookingWithDetails>> =
        bookingRepository.myBookings(page = page, size = size, status = status).map { paged ->
            val bookings = paged.content

            val salonNameById = bookings.map { it.salonId }.distinct()
                .associateWith { salonId -> salonRepository.getSalon(salonId).getOrNull()?.name }

            val specialistNameByKey = bookings.map { it.salonId to it.specialistId }.distinct()
                .associate { key -> key to specialistRepository.getSpecialist(key.first, key.second).getOrNull()?.displayName }

            PagedResult(
                content = bookings.map { booking ->
                    BookingWithDetails(
                        booking = booking,
                        salonName = salonNameById[booking.salonId],
                        specialistName = specialistNameByKey[booking.salonId to booking.specialistId],
                    )
                },
                page = paged.page,
                size = paged.size,
                totalElements = paged.totalElements,
                totalPages = paged.totalPages,
            )
        }
}
