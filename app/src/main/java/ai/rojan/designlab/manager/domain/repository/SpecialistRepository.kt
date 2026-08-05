package ai.rojan.designlab.manager.domain.repository

import ai.rojan.designlab.manager.domain.specialist.Specialist

interface SpecialistRepository {
    fun getAll(): List<Specialist>
    fun getById(id: String): Specialist?
    suspend fun create(specialist: Specialist): Result<Specialist>
    suspend fun update(specialist: Specialist): Result<Specialist?>
}
