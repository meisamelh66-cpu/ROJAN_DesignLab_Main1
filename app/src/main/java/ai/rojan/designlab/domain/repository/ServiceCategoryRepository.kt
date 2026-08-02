package ai.rojan.designlab.domain.repository

data class ServiceCategory(
    val id: String,
    val salonId: String,
    val name: String,
    val description: String?,
)

/** Talks to the ROJAN backend's Service Category API (`ROJAN_Backend/API_CONTRACT.md`). */
interface ServiceCategoryRepository {
    suspend fun getCategories(salonId: String): Result<List<ServiceCategory>>
}
