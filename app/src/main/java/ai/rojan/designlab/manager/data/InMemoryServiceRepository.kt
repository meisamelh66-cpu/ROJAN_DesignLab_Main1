package ai.rojan.designlab.manager.data

import ai.rojan.designlab.manager.domain.repository.ServiceRepository
import ai.rojan.designlab.manager.domain.service.Service

/**
 * In-memory [ServiceRepository] — new sample catalog (no prior Service
 * entity existed to migrate; names reuse the service labels already
 * appearing in Calendar/customer service-history sample strings, with
 * plausible price/duration invented for the Phase 1 foundation).
 */
class InMemoryServiceRepository : ServiceRepository {

    private val services = mutableListOf(
        Service("s1", "رنگ مو", "مو", price = 1_200_000, durationMinutes = 90, active = true),
        Service("s2", "کوتاهی مو", "مو", price = 450_000, durationMinutes = 45, active = true),
        Service("s3", "میکاپ عروس", "میکاپ", price = 2_500_000, durationMinutes = 120, active = true),
        Service("s4", "مانیکور", "ناخن", price = 350_000, durationMinutes = 40, active = true),
        Service("s5", "پدیکور", "ناخن", price = 400_000, durationMinutes = 50, active = true),
        Service("s6", "پاکسازی پوست", "پوست", price = 600_000, durationMinutes = 60, active = true),
        Service("s7", "میکاپ", "میکاپ", price = 800_000, durationMinutes = 60, active = true),
    )

    override fun getAll(): List<Service> = services.toList()

    override fun getById(id: String): Service? = services.find { it.id == id }

    override fun create(service: Service): Service {
        services.add(service)
        return service
    }

    override fun update(service: Service): Service? {
        val index = services.indexOfFirst { it.id == service.id }
        if (index == -1) return null
        services[index] = service
        return service
    }

    override fun delete(id: String): Boolean = services.removeIf { it.id == id }
}
