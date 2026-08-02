package ai.rojan.designlab.domain.repository

enum class BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED,
}

data class Booking(
    val id: String,
    val salonId: String,
    val serviceId: String,
    val specialistId: String,
    val customerId: String,
    val startTime: String,
    val endTime: String,
    val status: BookingStatus,
    val notes: String?,
)

/** Talks to the ROJAN backend's Booking API (`ROJAN_Backend/API_CONTRACT.md`). */
interface BookingRepository {

    /**
     * Creates a booking as the authenticated customer. [idempotencyKey],
     * when supplied, lets a retried request return the original booking
     * instead of creating a duplicate — see the "Idempotency" section of
     * `API_CONTRACT.md`. Pass a fresh key (e.g. a UUID) per distinct user
     * attempt, the same key again only for an automatic retry of that
     * exact attempt.
     */
    suspend fun createBooking(
        salonId: String,
        serviceId: String,
        specialistId: String,
        startTime: String,
        notes: String?,
        idempotencyKey: String?,
    ): Result<Booking>

    suspend fun myBookings(page: Int = 0, size: Int = 20, status: BookingStatus? = null): Result<PagedResult<Booking>>

    suspend fun getBooking(bookingId: String): Result<Booking>

    suspend fun cancelBooking(bookingId: String): Result<Booking>
}
