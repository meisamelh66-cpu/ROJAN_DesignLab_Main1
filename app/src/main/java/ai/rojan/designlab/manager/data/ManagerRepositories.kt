package ai.rojan.designlab.manager.data

import ai.rojan.designlab.manager.domain.repository.AppointmentRepository
import ai.rojan.designlab.manager.domain.repository.CustomerRepository
import ai.rojan.designlab.manager.domain.repository.ServiceRepository
import ai.rojan.designlab.manager.domain.repository.SpecialistRepository

/**
 * Minimal composition root for the Manager module's in-memory
 * repositories. No DI framework/ViewModel layer introduced this phase
 * (Phase 1 is data/architecture only) — screens read from these
 * singletons directly, the same "no backend, static data" posture the
 * Manager MVP has used throughout. Swapping in a real DI graph later
 * only requires changing this one object, not any call site's
 * repository-interface usage.
 */
object ManagerRepositories {
    val customers: CustomerRepository = InMemoryCustomerRepository()
    val appointments: AppointmentRepository = InMemoryAppointmentRepository()
    val services: ServiceRepository = InMemoryServiceRepository()
    val specialists: SpecialistRepository = InMemorySpecialistRepository()
}
