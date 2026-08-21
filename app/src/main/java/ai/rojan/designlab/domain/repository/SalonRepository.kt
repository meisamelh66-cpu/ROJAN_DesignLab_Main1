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
 * carry them through yet. [coverImageUrl] (Media Sprint P0) is the same
 * story - real on the wire (`SalonResponseDto.coverImageUrl`) since Central
 * Salon Management shipped, just never carried through to this domain
 * model or rendered anywhere until now.
 */
data class Salon(
    val id: String,
    val name: String,
    val description: String?,
    val phone: String,
    val email: String?,
    val address: String,
    val logoUrl: String? = null,
    val coverImageUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
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

    /**
     * Media Sprint P0: this salon's public gallery images, as plain URLs -
     * `GET /api/v1/salons/{salonId}/media?mediaType=GALLERY`. Authenticated
     * (like every non-public-prefixed endpoint) but not permission-gated
     * beyond that (`ListMediaUseCase`'s own doc comment: "intentionally
     * open to any authenticated caller"), so any logged-in customer viewing
     * a salon's details can read it. Ordering is whatever the backend
     * returns - no display-order concept exists yet.
     */
    suspend fun getGallery(salonId: String): Result<List<String>>
}
