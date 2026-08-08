package ai.rojan.designlab.domain.repository

/**
 * A [Booking] plus its salon/specialist display names, for appointment-
 * history UI. `Booking` itself only carries ids.
 *
 * Deliberately carries no service name or price: the backend's Service
 * API (`ServiceApi.getServices(salonId, categoryId)`) has no lookup-by-id,
 * only a per-category listing — resolving a service name from a bare
 * `serviceId` would mean scanning every category of every salon, an
 * unbounded operation, not a bounded per-unique-id lookup like salon/
 * specialist get. Left out rather than faked or turned into a scan.
 * **Backend follow-up required:** a get-service-by-id (or batch) endpoint.
 */
data class BookingWithDetails(
    val booking: Booking,
    val salonName: String?,
    val specialistName: String?,
)

/**
 * Read-only composition over [BookingRepository]/[SalonRepository]/
 * [SpecialistRepository] that resolves each returned page's bookings to
 * their salon/specialist names via a bounded, deduplicated lookup — one
 * call per *unique* salon id and one per unique (salonId, specialistId)
 * pair in the page, never one per booking (no N+1 relative to booking
 * count).
 */
interface BookingHistoryRepository {

    suspend fun myBookingsWithDetails(
        page: Int = 0,
        size: Int = 20,
        status: BookingStatus? = null,
    ): Result<PagedResult<BookingWithDetails>>
}
