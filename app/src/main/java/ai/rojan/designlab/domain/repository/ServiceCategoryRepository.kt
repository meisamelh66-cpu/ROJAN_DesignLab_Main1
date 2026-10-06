package ai.rojan.designlab.domain.repository

data class ServiceCategory(
    val id: String,
    val salonId: String,
    val name: String,
    val description: String?,
    // Salon Data Integration (Master Integration Repair, PASS 2): real
    // backend field (`ServiceCategoryResponseDto.active`), previously dropped
    // by this mapper — carried through for completeness; no new client-side
    // filtering is applied anywhere.
    val active: Boolean = true,
)

/** Talks to the ROJAN backend's Service Category API (`ROJAN_Backend/API_CONTRACT.md`). */
interface ServiceCategoryRepository {
    suspend fun getCategories(salonId: String): Result<List<ServiceCategory>>
}
