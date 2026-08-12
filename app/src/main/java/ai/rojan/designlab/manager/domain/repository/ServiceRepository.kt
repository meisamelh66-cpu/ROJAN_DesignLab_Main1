package ai.rojan.designlab.manager.domain.repository

import ai.rojan.designlab.manager.domain.service.Service

/**
 * `getAll`/`getById` stay synchronous, backed by an in-memory cache the
 * implementation refreshes from the backend via its own `sync()` method
 * (not part of this interface — see
 * [ai.rojan.designlab.manager.data.BackendServiceRepository]) — every
 * existing call site (Calendar's service-name lookup, the booking
 * wizard's `activeServices()`/`serviceById()`) reads synchronously today
 * and none of them are on a path where a network round-trip per call
 * would be acceptable. `create`/`update`/`delete` are `suspend` because
 * they now hit a real, owner-authenticated backend endpoint
 * (`ServiceController`) and have no existing caller yet to be broken by
 * that (no Services management screen has been built).
 */
interface ServiceRepository {
    fun getAll(): List<Service>
    fun getById(id: String): Service?
    suspend fun create(service: Service): Result<Service>
    suspend fun update(service: Service): Result<Service?>
    suspend fun delete(id: String): Result<Boolean>

    /** Every category name [create]/[update] can resolve to a real `categoryId` for — populated by the same `sync()` that populates [getAll], no extra network call. A create/edit form must pick from this list, never free-type a name (there is no category-creation endpoint). */
    fun getCategoryNames(): List<String>
}
