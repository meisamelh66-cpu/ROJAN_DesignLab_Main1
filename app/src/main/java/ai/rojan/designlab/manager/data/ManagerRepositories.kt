package ai.rojan.designlab.manager.data

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus
import ai.rojan.designlab.manager.domain.repository.AppointmentRepository
import ai.rojan.designlab.manager.domain.repository.CustomerRepository
import ai.rojan.designlab.manager.domain.repository.ServiceRepository
import ai.rojan.designlab.manager.domain.repository.SpecialistRepository
import ai.rojan.designlab.manager.domain.service.Service
import android.content.Context

/** Empty until [ManagerRepositories.initialize] resolves a real salon - honest "nothing loaded yet," not fake sample data. */
private object EmptyServiceRepository : ServiceRepository {
    override fun getAll(): List<Service> = emptyList()
    override fun getById(id: String): Service? = null
    override suspend fun create(service: Service): Result<Service> =
        Result.failure(IllegalStateException("ManagerRepositories.initialize() has not completed yet"))
    override suspend fun update(service: Service): Result<Service?> =
        Result.failure(IllegalStateException("ManagerRepositories.initialize() has not completed yet"))
    override suspend fun delete(id: String): Result<Boolean> =
        Result.failure(IllegalStateException("ManagerRepositories.initialize() has not completed yet"))
}

/** Empty until [ManagerRepositories.initialize] resolves a real salon - honest "nothing loaded yet," not fake sample data. */
private object EmptyAppointmentRepository : AppointmentRepository {
    override fun getAll(): List<Appointment> = emptyList()
    override fun getById(id: String): Appointment? = null
    override fun getByCustomerId(customerId: String): List<Appointment> = emptyList()
    override fun create(appointment: Appointment): Appointment = appointment
    override fun update(appointment: Appointment): Appointment? = null
    override fun updateStatus(id: String, status: AppointmentStatus): Appointment? = null
    override fun cancel(id: String): Appointment? = null
}

/**
 * Composition root for the Manager module's repositories (Manager App
 * Phase 2 - Services + Appointment Integration). [customers]/[specialists]
 * remain in-memory - explicitly out of scope for this phase, since no
 * backend Customer/CRM API exists yet (see `ROJAN_Manager_App_Audit_Report.md`).
 * [services]/[appointments] are now real, backend-backed
 * ([BackendServiceRepository]/[BackendAppointmentRepository]).
 *
 * [initialize] must be called once - from
 * [ai.rojan.designlab.ManagerActivity], in a coroutine - before
 * [services]/[appointments] have real data; until then they're the empty
 * objects above. That is a genuine, honest gap versus the old always-ready
 * in-memory sample data: a real network round-trip cannot be instant.
 * Whichever screen first reads them may see an empty list for a moment on
 * cold start - this composition root does not attempt to solve the
 * Compose-recomposition-on-async-load problem for every existing call
 * site (Calendar's direct, non-`remember` reads in particular); that is a
 * real, separate follow-up for whoever wires this to an actual device/
 * emulator run, not something guessed at blind here.
 */
object ManagerRepositories {
    val customers: CustomerRepository = InMemoryCustomerRepository()
    val specialists: SpecialistRepository = InMemorySpecialistRepository()

    var services: ServiceRepository = EmptyServiceRepository
        private set
    var appointments: AppointmentRepository = EmptyAppointmentRepository
        private set

    /**
     * Resolves the authenticated owner's salon (`GET /api/v1/salons/mine`,
     * first result - multi-branch selection is out of scope) and syncs
     * real Service/Appointment data. Safe to call again to re-sync.
     */
    suspend fun initialize(context: Context): Result<Unit> {
        val container = BackendApiContainerHolder.get(context)
        val salon = runCatching { container.managerSalonApi.mine() }
            .getOrElse { return Result.failure(it) }
            .firstOrNull()
            ?: return Result.failure(IllegalStateException("No salon found for this account (GET /salons/mine returned none)"))

        val serviceRepo = BackendServiceRepository(
            serviceApi = container.serviceApi,
            serviceCategoryApi = container.serviceCategoryApi,
            managerServiceApi = container.managerServiceApi,
            salonId = salon.id,
        )
        val appointmentRepo = BackendAppointmentRepository(
            managerBookingApi = container.managerBookingApi,
            salonId = salon.id,
        )

        val serviceSync = serviceRepo.sync()
        val appointmentSync = appointmentRepo.sync()

        services = serviceRepo
        appointments = appointmentRepo

        return serviceSync.fold(
            onSuccess = { appointmentSync },
            onFailure = { Result.failure(it) },
        )
    }
}
