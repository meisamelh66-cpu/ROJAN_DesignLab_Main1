package ai.rojan.designlab.data.repository

import ai.rojan.designlab.domain.repository.BookingHistoryRepository
import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.domain.repository.BookingWithDetails
import ai.rojan.designlab.domain.repository.PagedResult
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.repository.SpecialistRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

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

            // Root-cause fix (Customer Regression Pass): each distinct salon/
            // specialist lookup below used to run sequentially (one
            // `associateWith`/`associate` awaiting the previous call before the
            // next started) - for a customer with N distinct salons/specialists
            // across their booking history, that's N+M fully serial network
            // round-trips every time Home/Profile loads this list, each one
            // individually bounded by BackendApiContainer's NETWORK_TIMEOUT
            // (30s) - so a single slow/unreachable salon or specialist could
            // stall the whole section for up to N x 30s, reading as "loading
            // forever". Running them concurrently via `async` bounds the total
            // wait to the single slowest call instead of their sum - same
            // data, same fallback-to-null-on-failure behavior, just parallel.
            val (salonNameById, specialistNameByKey) = coroutineScope {
                val salonNameDeferred = bookings.map { it.salonId }.distinct()
                    .associateWith { salonId -> async { salonRepository.getSalon(salonId).getOrNull()?.name } }

                val specialistNameDeferred = bookings.map { it.salonId to it.specialistId }.distinct()
                    .associateWith { key -> async { specialistRepository.getSpecialist(key.first, key.second).getOrNull()?.displayName } }

                salonNameDeferred.mapValues { it.value.await() } to specialistNameDeferred.mapValues { it.value.await() }
            }

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
