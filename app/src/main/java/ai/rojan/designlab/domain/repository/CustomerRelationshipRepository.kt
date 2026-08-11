package ai.rojan.designlab.domain.repository

/** Mirrors the backend `SalonFollowStatus` enum (`ROJAN_Backend` commit db5faea). The list endpoint only ever returns ACTIVE rows, but the mutation response can reflect either. */
enum class SalonFollowStatus {
    ACTIVE,
    REMOVED,
}

data class CustomerFollowedSalon(
    val id: String,
    val salonId: String,
    val status: SalonFollowStatus,
    val createdAt: String,
)

data class CustomerFavoriteSalon(
    val id: String,
    val salonId: String,
    val createdAt: String,
)

/**
 * Talks to the ROJAN backend's Customer Relationship API - follow (updates/
 * news intent) and favorite (personal-bookmark intent) are kept as separate
 * operations throughout this interface, mirroring the backend's own
 * deliberate separation of `SalonFollow`/`SalonFavorite` (see
 * `ROJAN_Backend`'s `SalonFollow.kt`/`SalonFavorite.kt` doc comments) -
 * never merge them into one "relationship type" concept.
 *
 * Every method is implicitly scoped to the authenticated caller - there is
 * no customerId parameter anywhere on this interface, matching the backend
 * resolving identity from the JWT alone.
 */
interface CustomerRelationshipRepository {
    suspend fun followSalon(salonId: String): Result<CustomerFollowedSalon>
    suspend fun unfollowSalon(salonId: String): Result<Unit>
    suspend fun getFollowedSalons(page: Int = 0, size: Int = 20): Result<List<CustomerFollowedSalon>>

    suspend fun favoriteSalon(salonId: String): Result<CustomerFavoriteSalon>
    suspend fun unfavoriteSalon(salonId: String): Result<Unit>
    suspend fun getFavoriteSalons(page: Int = 0, size: Int = 20): Result<List<CustomerFavoriteSalon>>
}
