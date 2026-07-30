package ai.rojan.designlab.manager.domain.repository

import ai.rojan.designlab.manager.domain.service.Service

interface ServiceRepository {
    fun getAll(): List<Service>
    fun getById(id: String): Service?
    fun create(service: Service): Service
    fun update(service: Service): Service?
    fun delete(id: String): Boolean
}
