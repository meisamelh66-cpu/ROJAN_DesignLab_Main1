package ai.rojan.designlab.manager.data

import ai.rojan.designlab.data.remote.dto.DashboardInsightsResponseDto
import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus
import ai.rojan.designlab.manager.domain.customer.CustomerServiceHistoryEntry
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
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

/** Empty until [ManagerRepositories.initialize] resolves a real salon - honest "nothing loaded yet," not fake sample data. */
private object EmptyCustomerRepository : CustomerRepository {
    override fun getAll(): List<ManagerCustomer> = emptyList()
    override fun getById(id: String): ManagerCustomer? = null
    override fun search(query: String): List<ManagerCustomer> = emptyList()
    override suspend fun create(customer: ManagerCustomer): Result<ManagerCustomer> =
        Result.failure(IllegalStateException("ManagerRepositories.initialize() has not completed yet"))
    override suspend fun update(customer: ManagerCustomer): Result<ManagerCustomer?> =
        Result.failure(IllegalStateException("ManagerRepositories.initialize() has not completed yet"))
    override fun getServiceHistory(customerId: String): List<CustomerServiceHistoryEntry> = emptyList()
}

/**
 * Composition root for the Manager module's repositories. [specialists]
 * remains in-memory - no backend Specialist-management API was in scope
 * for any phase so far. [services]/[appointments]/[customers] are real,
 * backend-backed ([BackendServiceRepository]/[BackendAppointmentRepository]/
 * [BackendCustomerRepository] — the last added in Final Backend
 * Integration, replacing the removed `InMemoryCustomerRepository`).
 * [dashboardInsights] is likewise real, from `GET /api/v1/dashboard/insights`
 * (`DashboardController`) — null until [initialize] completes, or if that
 * one call fails independently (see [initialize]'s doc comment); consumers
 * (`AIInsightCard`) must treat null as "no data yet," not an error.
 *
 * [initialize] must be called once - from
 * [ai.rojan.designlab.ManagerActivity], in a coroutine - before
 * [services]/[appointments]/[customers]/[dashboardInsights] have real data;
 * until then they're the empty objects/null above. That is a genuine,
 * honest gap versus the old always-ready in-memory sample data: a real
 * network round-trip cannot be instant. Whichever screen first reads them
 * may see an empty list for a moment on cold start - this composition root
 * does not attempt to solve the Compose-recomposition-on-async-load
 * problem for every existing call site (Calendar's direct, non-`remember`
 * reads in particular); that is a real, separate follow-up for whoever
 * wires this to an actual device/emulator run, not something guessed at
 * blind here.
 */
object ManagerRepositories {
    val specialists: SpecialistRepository = InMemorySpecialistRepository()

    var services: ServiceRepository = EmptyServiceRepository
        private set
    var appointments: AppointmentRepository = EmptyAppointmentRepository
        private set
    var customers: CustomerRepository = EmptyCustomerRepository
        private set
    var dashboardInsights: DashboardInsightsResponseDto? = null
        private set

    /**
     * Resolves the authenticated owner's salon (`GET /api/v1/salons/mine`,
     * first result - multi-branch selection is out of scope) and syncs
     * real Service/Appointment/Customer data, plus Dashboard Insights.
     * Safe to call again to re-sync.
     *
     * Insights failing (401/404/409 - see `ManagerDashboardApi`'s own doc
     * comment for what each means) does not fail the whole call: it's
     * fetched independently of salon/service/appointment/customer sync, so
     * one genuinely-optional card being unavailable doesn't block the rest
     * of the dashboard.
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
        val customerRepo = BackendCustomerRepository(
            managerCustomerApi = container.managerCustomerApi,
            salonId = salon.id,
        )

        val serviceSync = serviceRepo.sync()
        val appointmentSync = appointmentRepo.sync()
        val customerSync = customerRepo.sync()
        dashboardInsights = runCatching { container.managerDashboardApi.insights() }.getOrNull()

        services = serviceRepo
        appointments = appointmentRepo
        customers = customerRepo

        return serviceSync
            .fold(onSuccess = { appointmentSync }, onFailure = { Result.failure(it) })
            .fold(onSuccess = { customerSync }, onFailure = { Result.failure(it) })
    }
}
