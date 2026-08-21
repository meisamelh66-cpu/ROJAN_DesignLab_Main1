package ai.rojan.designlab.domain.repository

data class Service(
    val id: String,
    val salonId: String,
    val categoryId: String,
    val name: String,
    val description: String?,
    val durationMinutes: Int,
    val price: Double,
)

/** Talks to the ROJAN backend's Service API (`ROJAN_Backend/API_CONTRACT.md`). */
interface ServiceRepository {
    suspend fun getServices(salonId: String, categoryId: String): Result<List<Service>>

    /** Media System Evolution v2: this service's images, as plain URLs, pre-sorted by the backend's own display order. */
    suspend fun getImages(salonId: String, serviceId: String): Result<List<String>>
}
