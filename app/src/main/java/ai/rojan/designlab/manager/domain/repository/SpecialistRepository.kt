package ai.rojan.designlab.manager.domain.repository

import ai.rojan.designlab.manager.domain.specialist.Specialist

interface SpecialistRepository {
    fun getAll(): List<Specialist>
    fun getById(id: String): Specialist?
    suspend fun create(specialist: Specialist): Result<Specialist>
    suspend fun update(specialist: Specialist): Result<Specialist?>

    /** Deactivates the specialist on the real backend (`ManagerSpecialistApi.deactivate`) — same "delete" naming/shape as [ai.rojan.designlab.manager.domain.repository.ServiceRepository.delete], kept symmetric across the two nearly-identical repositories. */
    suspend fun delete(id: String): Result<Boolean>

    /**
     * P1-5, Manager Specialist <-> Service Assignment: the real service ids
     * this specialist is eligible to perform (`GET .../services`) — empty
     * means eligible for every service in the salon, the same business rule
     * [ai.rojan.designlab.domain.repository.SpecialistRepository.getAssignedServiceIds]
     * (Customer-side) already established; never treat empty as "assigned
     * to nothing".
     */
    suspend fun getAssignedServiceIds(specialistId: String): Result<List<String>>

    /** Assigns [serviceId] to [specialistId] (`PUT .../services/{serviceId}`) — restricts them to their assigned set going forward. */
    suspend fun assignService(specialistId: String, serviceId: String): Result<Unit>

    /** Removes [serviceId] from [specialistId]'s assignments (`DELETE .../services/{serviceId}`). */
    suspend fun unassignService(specialistId: String, serviceId: String): Result<Unit>
}
