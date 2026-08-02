package ai.rojan.designlab.domain.repository

/**
 * Domain-facing shape of a salon. Deliberately narrower than
 * [ai.rojan.designlab.data.demo.DemoSalon] — the backend has no
 * rating/review-aggregate, distance, working-hours-as-a-display-string, or
 * image concept, so those stay screen-level/demo-only rather than being
 * fabricated here.
 */
data class Salon(
    val id: String,
    val name: String,
    val description: String?,
    val phone: String,
    val email: String?,
    val address: String,
)

/** Talks to the ROJAN backend's Salon API (`ROJAN_Backend/API_CONTRACT.md`). */
interface SalonRepository {

    /** Browses active salons. [nameFilter], when non-null/blank, matches a case-insensitive name substring. */
    suspend fun browseSalons(page: Int = 0, size: Int = 20, nameFilter: String? = null): Result<PagedResult<Salon>>

    suspend fun getSalon(salonId: String): Result<Salon>
}
