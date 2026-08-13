package ai.rojan.designlab.domain.repository

enum class BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED,
}

/** A booking-list display name — [ServiceSummary]/[SpecialistSummary]/[CustomerSummary] below share this shape. */
data class BookingParticipantSummary(val id: String, val name: String)

typealias ServiceSummary = BookingParticipantSummary
typealias SpecialistSummary = BookingParticipantSummary

/** [phone] is omitted server-side wherever the caller's CRM permission tier doesn't warrant it — never assumed present. */
data class CustomerSummary(val id: String, val name: String, val phone: String?)

/**
 * [service]/[specialist]/[customer] are the enrichment
 * `ROJAN_System1_Backend_Decision_v2.md` §3 approved, additively, on top
 * of the existing flat [serviceId]/[specialistId]/[customerId] (kept,
 * unchanged, for backward compatibility). All three are nullable
 * deliberately: the backend has not shipped this enrichment yet (verified
 * against `origin/feature/auth-rate-limit-finalization`) — every
 * `BookingResponseDto` received today omits them, and this type must not
 * pretend otherwise. Populate a booking list from [id]/[serviceId]/etc.
 * until these are reliably non-null; do not synthesize a placeholder name
 * when they're absent.
 */
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
    val service: ServiceSummary? = null,
    val specialist: SpecialistSummary? = null,
    val customer: CustomerSummary? = null,
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

    /**
     * `PATCH /bookings/{bookingId}/confirm` — owner only today; Reception
     * needs `MANAGE_BOOKINGS`, not yet broadened backend-side (see
     * `ROJAN_System1_Backend_Decision_v2.md` §4 item 6). The endpoint
     * itself already existed backend-side — only this binding was missing.
     */
    suspend fun confirmBooking(bookingId: String): Result<Booking>

    /** `PATCH /bookings/{bookingId}/complete` — same status as [confirmBooking]. */
    suspend fun completeBooking(bookingId: String): Result<Booking>

    /**
     * Moves an existing booking to [newStartTime] (ISO-8601 local date-time,
     * one of the windows returned by the availability endpoint — same
     * shape [createBooking]'s [startTime] already expects). Its own
     * customer or the salon owner/manager/receptionist may call this;
     * the backend re-checks the new time for conflicts.
     */
    suspend fun rescheduleBooking(bookingId: String, newStartTime: String): Result<Booking>
}
