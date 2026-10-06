package ai.rojan.designlab.domain.repository

data class Service(
    val id: String,
    val salonId: String,
    val categoryId: String,
    val name: String,
    val description: String?,
    val durationMinutes: Int,
    val price: Double,
    // Salon Data Integration (Master Integration Repair, PASS 2): a real
    // backend field (`ServiceResponseDto.active`) previously dropped by this
    // mapper. The authenticated list endpoint this repository calls is not
    // itself filtered by active/inactive server-side — carried through here
    // so a screen CAN show/label an inactive service; this does not add any
    // new client-side filtering decision (none is applied anywhere).
    val active: Boolean = true,
)

/** Talks to the ROJAN backend's Service API (`ROJAN_Backend/API_CONTRACT.md`). */
interface ServiceRepository {
    suspend fun getServices(salonId: String, categoryId: String): Result<List<Service>>
}
