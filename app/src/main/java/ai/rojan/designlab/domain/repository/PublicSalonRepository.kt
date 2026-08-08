package ai.rojan.designlab.domain.repository

/** Public (unauthenticated) view of a salon, resolved by its `slug` — never carries `ownerId`/internal linkage fields. */
data class PublicSalon(
    val id: String,
    val name: String,
    val description: String?,
    val phone: String,
    val address: String,
    val logoUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
)

data class PublicServiceCategory(
    val id: String,
    val name: String,
    val description: String?,
)

data class PublicService(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String?,
    val durationMinutes: Int,
    val price: Double,
)

data class PublicSpecialist(
    val id: String,
    val displayName: String,
    val bio: String?,
    val photoUrl: String?,
)

/**
 * Talks to the ROJAN backend's unauthenticated QR-code customer journey
 * (`ROJAN_Backend/PublicSalonController`) — the entry point for a
 * customer scanning a salon's QR code before/without logging in. Every
 * method resolves the salon by its public `slug`, not an id.
 *
 * Data layer only this phase (Phase 2, C3): no screen consumes this yet —
 * the QR-scan/deep-link entry-point UI is a separate, later decision.
 */
interface PublicSalonRepository {

    suspend fun getSalon(slug: String): Result<PublicSalon>

    suspend fun getCategories(slug: String): Result<List<PublicServiceCategory>>

    suspend fun getServices(slug: String, categoryId: String): Result<List<PublicService>>

    suspend fun getSpecialists(slug: String): Result<List<PublicSpecialist>>

    suspend fun getAvailableSlots(
        slug: String,
        specialistId: String,
        serviceId: String,
        date: String,
        slotIntervalMinutes: Int = 15,
    ): Result<List<TimeSlot>>
}
