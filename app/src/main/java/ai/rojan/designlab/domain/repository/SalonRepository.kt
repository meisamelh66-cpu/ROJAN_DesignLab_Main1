package ai.rojan.designlab.domain.repository

/**
 * Salon Discovery: structured geo/address fields for one salon - a real
 * [ai.rojan.backend.domain.salon.Salon.latitude]/`longitude` pair now
 * flows through from the backend (confirmed present on the wire, just
 * never mapped through on the Android side before). [city] stays null -
 * the backend only ever stores a single free-text `address` string, no
 * separate city field to derive it from without guessing.
 *
 * Deliberately not wired to any maps rendering or device-location lookup
 * yet - no maps dependency exists in this app, and there is no on-device
 * "where is the customer right now" source to compute a real distance
 * from. This is the future-ready structure only; a distance figure would
 * be fabricated without both of those, so none is shown anywhere yet.
 */
data class SalonLocation(
    val latitude: Double?,
    val longitude: Double?,
    val address: String,
    val city: String? = null,
)

/**
 * Domain-facing shape of a salon. Deliberately narrower than
 * [ai.rojan.designlab.data.demo.DemoSalon] — the backend has no
 * rating/review-aggregate or working-hours-as-a-display-string concept, so
 * those stay screen-level/demo-only rather than being fabricated here.
 * [logoUrl]/[latitude]/[longitude] were added for Salon Discovery - the
 * backend `Salon` entity already had them, this domain model just didn't
 * carry them through yet.
 */
data class Salon(
    val id: String,
    val name: String,
    val description: String?,
    val phone: String,
    val email: String?,
    val address: String,
    val logoUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    // TEAM2-002 (Manager Data Persistence): defaulted so every existing
    // Salon(...) construction (production and test) stays source-compatible.
    // Customer-facing discovery (browseSalons/getSalon) is unaffected by
    // this field — the backend's own query already scopes those to active
    // salons; this is exposed so Manager-side reads (myOwnedSalons, the
    // Dashboard identity card) can tell an active salon from a draft one.
    val active: Boolean = true,
) {
    val location: SalonLocation get() = SalonLocation(latitude = latitude, longitude = longitude, address = address)
}

/** Talks to the ROJAN backend's Salon API (`ROJAN_Backend/API_CONTRACT.md`). */
interface SalonRepository {

    /**
     * Browses active salons, paginated. [nameFilter], when non-null/blank,
     * matches a case-insensitive name substring - the only search field
     * the backend supports (confirmed: no service-name/specialist-name/
     * location search exists server-side). [sortDirection] mirrors the
     * backend's own `SortDirection` (`"ASC"`/`"DESC"`, sorted by name only
     * - the only sort key the backend supports).
     */
    suspend fun browseSalons(
        page: Int = 0,
        size: Int = 20,
        nameFilter: String? = null,
        sortDirection: String = "ASC",
    ): Result<PagedResult<Salon>>

    suspend fun getSalon(salonId: String): Result<Salon>

    /** TEAM2-002. Salons owned by the currently authenticated account — `GET /api/v1/salons/mine`. Not paginated (an owner's own salon count is always small). */
    suspend fun myOwnedSalons(): Result<List<Salon>>
}
