package ai.rojan.designlab.domain.repository

/**
 * Domain-facing shape of a specialist. Deliberately narrower than
 * [ai.rojan.designlab.data.demo.DemoSpecialist] — the backend has no
 * rating/review-aggregate, experience-years, skills, languages, or
 * completed-appointments concept, so those stay screen-level/demo-only
 * rather than being fabricated here.
 */
data class Specialist(
    val id: String,
    val salonId: String,
    val displayName: String,
    val bio: String?,
    val photoUrl: String?,
)

/** Talks to the ROJAN backend's Specialist API (`ROJAN_Backend/API_CONTRACT.md`). */
interface SpecialistRepository {
    suspend fun getSpecialists(salonId: String): Result<List<Specialist>>
    suspend fun getSpecialist(salonId: String, specialistId: String): Result<Specialist>

    /** Media System Evolution v2: this specialist's portfolio images, as plain URLs, pre-sorted by the backend's own display order. */
    suspend fun getPortfolio(salonId: String, specialistId: String): Result<List<String>>

    /**
     * Customer Specialist -> Services Integration: the real service ids this
     * specialist is eligible to perform, per `SpecialistController`'s
     * `GET /specialists/{id}/services`. **Empty means eligible for every
     * service in the salon** - confirmed by reading `SpecialistController.kt`
     * directly, never "assigned to nothing". Callers must preserve this
     * distinction rather than treating an empty list as a restriction.
     */
    suspend fun getAssignedServiceIds(salonId: String, specialistId: String): Result<List<String>>
}
