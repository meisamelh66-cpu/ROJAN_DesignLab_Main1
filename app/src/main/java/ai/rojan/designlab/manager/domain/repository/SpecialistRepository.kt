package ai.rojan.designlab.manager.domain.repository

import ai.rojan.designlab.manager.domain.specialist.Specialist

interface SpecialistRepository {
    fun getAll(): List<Specialist>
    fun getById(id: String): Specialist?
    suspend fun create(specialist: Specialist): Result<Specialist>
    suspend fun update(specialist: Specialist): Result<Specialist?>

    /** Deactivates the specialist on the real backend (`ManagerSpecialistApi.deactivate`) — same "delete" naming/shape as [ai.rojan.designlab.manager.domain.repository.ServiceRepository.delete], kept symmetric across the two nearly-identical repositories. */
    suspend fun delete(id: String): Result<Boolean>

    /** Real per-specialist service-eligibility ids (`ManagerSpecialistApi.eligibleServiceIds`) — empty means eligible for every service in the salon, per the backend's own contract. The real substitute for the old free-text `skills` field. */
    suspend fun eligibleServiceIds(specialistId: String): Result<List<String>>
}
