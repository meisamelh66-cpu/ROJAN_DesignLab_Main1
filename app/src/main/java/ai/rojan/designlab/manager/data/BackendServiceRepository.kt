package ai.rojan.designlab.manager.data

import ai.rojan.designlab.data.remote.ManagerServiceApi
import ai.rojan.designlab.data.remote.ServiceApi
import ai.rojan.designlab.data.remote.ServiceCategoryApi
import ai.rojan.designlab.data.remote.dto.CreateServiceRequest
import ai.rojan.designlab.data.remote.dto.ServiceResponseDto
import ai.rojan.designlab.data.remote.dto.UpdateServiceRequest
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.manager.domain.repository.ServiceRepository
import ai.rojan.designlab.manager.domain.service.Service

/**
 * Real backend-backed [ServiceRepository] (Manager App Phase 2 — Services
 * Integration). `getAll()`/`getById()` read an in-memory cache populated
 * by [sync] — see the interface's own doc comment for why those two stay
 * synchronous. The backend has no "all services for a salon" endpoint,
 * only per-category, so [sync] fetches every category first.
 *
 * `create`/`update`/`delete` have no real caller yet (no Services
 * management screen exists) — implemented against the real,
 * owner-authenticated `ServiceController` regardless, so the moment that
 * screen is built it has something real to call. [Service.category]
 * stores a category *name* (matching the existing domain model, unchanged
 * from its in-memory-era shape) — the category id a write call actually
 * needs is resolved from the name via the cache [sync] built, which is
 * why `create`/`update`/`delete` fail clearly if called before any
 * [sync].
 */
class BackendServiceRepository(
    private val serviceApi: ServiceApi,
    private val serviceCategoryApi: ServiceCategoryApi,
    private val managerServiceApi: ManagerServiceApi,
    private val salonId: String,
) : ServiceRepository {

    private var cache: List<Service> = emptyList()
    private val categoryIdByName = mutableMapOf<String, String>()
    private val categoryIdByServiceId = mutableMapOf<String, String>()

    /** Fetches every category's services for [salonId] and repopulates the cache. Call before first read, and to refresh. */
    suspend fun sync(): Result<Unit> = safeApiCall {
        val categories = serviceCategoryApi.getCategories(salonId)
        categoryIdByName.clear()
        categoryIdByServiceId.clear()
        val services = mutableListOf<Service>()
        for (category in categories) {
            categoryIdByName[category.name] = category.id
            for (dto in serviceApi.getServices(salonId, category.id)) {
                categoryIdByServiceId[dto.id] = category.id
                services += dto.toDomain(category.name)
            }
        }
        cache = services
    }

    override fun getAll(): List<Service> = cache

    override fun getById(id: String): Service? = cache.find { it.id == id }

    override suspend fun create(service: Service): Result<Service> {
        val categoryId = categoryIdByName[service.category]
            ?: return Result.failure(
                IllegalArgumentException("Unknown category \"${service.category}\" for salon $salonId - call sync() first"),
            )
        return safeApiCall {
            managerServiceApi.create(
                salonId = salonId,
                categoryId = categoryId,
                request = CreateServiceRequest(
                    name = service.name,
                    durationMinutes = service.durationMinutes,
                    price = service.price.toDouble(),
                ),
            )
        }.map { dto ->
            categoryIdByServiceId[dto.id] = categoryId
            dto.toDomain(service.category).also { created -> cache = cache + created }
        }
    }

    override suspend fun update(service: Service): Result<Service?> {
        val categoryId = categoryIdByServiceId[service.id]
            ?: return Result.failure(IllegalArgumentException("Unknown service id \"${service.id}\" - call sync() first"))
        return safeApiCall {
            managerServiceApi.update(
                salonId = salonId,
                categoryId = categoryId,
                serviceId = service.id,
                request = UpdateServiceRequest(
                    name = service.name,
                    durationMinutes = service.durationMinutes,
                    price = service.price.toDouble(),
                ),
            )
        }.map { dto ->
            dto.toDomain(service.category).also { updated ->
                cache = cache.map { if (it.id == updated.id) updated else it }
            }
        }
    }

    override suspend fun delete(id: String): Result<Boolean> {
        val categoryId = categoryIdByServiceId[id]
            ?: return Result.failure(IllegalArgumentException("Unknown service id \"$id\" - call sync() first"))
        return safeApiCall {
            managerServiceApi.deactivate(salonId, categoryId, id)
        }.map {
            cache = cache.filterNot { it.id == id }
            true
        }
    }

    private fun ServiceResponseDto.toDomain(categoryName: String) = Service(
        id = id,
        name = name,
        category = categoryName,
        price = price.toLong(),
        durationMinutes = durationMinutes,
        active = active,
    )
}
