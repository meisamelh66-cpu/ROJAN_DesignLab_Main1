package ai.rojan.designlab.manager.domain.repository

import ai.rojan.designlab.manager.domain.specialist.Specialist

interface SpecialistRepository {
    fun getAll(): List<Specialist>
    fun getById(id: String): Specialist?
    fun create(specialist: Specialist): Specialist
    fun update(specialist: Specialist): Specialist?
}
