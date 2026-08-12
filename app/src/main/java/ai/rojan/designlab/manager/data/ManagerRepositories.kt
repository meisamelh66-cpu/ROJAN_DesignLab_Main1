package ai.rojan.designlab.manager.data

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.repository.AvailabilityRepository
import ai.rojan.designlab.manager.domain.ai.ManagerCrmInsight
import ai.rojan.designlab.manager.domain.ai.ManagerCrmInsightContext
import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus
import ai.rojan.designlab.manager.domain.customer.CustomerNote
import ai.rojan.designlab.manager.domain.customer.CustomerServiceHistoryEntry
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import ai.rojan.designlab.manager.domain.dashboard.ManagerDashboardInsights
import ai.rojan.designlab.manager.domain.dashboard.ManagerSalonSummary
import ai.rojan.designlab.manager.domain.repository.AppointmentRepository
import ai.rojan.designlab.manager.domain.repository.CustomerRepository
import ai.rojan.designlab.manager.domain.repository.ServiceRepository
import ai.rojan.designlab.manager.domain.repository.SpecialistRepository
import ai.rojan.designlab.manager.domain.service.Service
import ai.rojan.designlab.manager.domain.specialist.Specialist
import android.content.Context
import kotlinx.coroutines.flow.first

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
    override fun getCategoryNames(): List<String> = emptyList()
}

/** Empty until [ManagerRepositories.initialize] resolves a real salon - honest "nothing loaded yet," not fake sample data. */
private object EmptyAppointmentRepository : AppointmentRepository {
    override fun getAll(): List<Appointment> = emptyList()
    override fun getById(id: String): Appointment? = null
    override fun getByCustomerId(customerId: String): List<Appointment> = emptyList()
    override suspend fun create(appointment: Appointment): Result<Appointment> =
        Result.failure(IllegalStateException("ManagerRepositories.initialize() has not completed yet"))
    override fun update(appointment: Appointment): Appointment? = null
    override fun updateStatus(id: String, status: AppointmentStatus): Appointment? = null
    override fun cancel(id: String): Appointment? = null
    override suspend fun createForCustomer(
        customerId: String,
        serviceId: String,
        specialistId: String,
        startTime: String,
        notes: String?,
    ): Result<Appointment> =
        Result.failure(IllegalStateException("ManagerRepositories.initialize() has not completed yet"))
}

/** Empty until [ManagerRepositories.initialize] resolves a real salon - honest "nothing loaded yet," not fake sample data. */
private object EmptySpecialistRepository : SpecialistRepository {
    override fun getAll(): List<Specialist> = emptyList()
    override fun getById(id: String): Specialist? = null
    override suspend fun create(specialist: Specialist): Result<Specialist> =
        Result.failure(IllegalStateException("ManagerRepositories.initialize() has not completed yet"))
    override suspend fun update(specialist: Specialist): Result<Specialist?> =
        Result.failure(IllegalStateException("ManagerRepositories.initialize() has not completed yet"))
    override suspend fun delete(id: String): Result<Boolean> =
        Result.failure(IllegalStateException("ManagerRepositories.initialize() has not completed yet"))
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
    override fun getNoteHistory(customerId: String): List<CustomerNote> = emptyList()
    override suspend fun loadDetail(customerId: String): Result<Unit> =
        Result.failure(IllegalStateException("ManagerRepositories.initialize() has not completed yet"))
}

/**
 * Composition root for the Manager module's repositories.
 * [services]/[appointments]/[customers]/[specialists] are all real,
 * backend-backed ([BackendServiceRepository]/[BackendAppointmentRepository]/
 * [BackendCustomerRepository]/[BackendSpecialistRepository] — the last
 * added in Final Release Validation, replacing the removed
 * `InMemorySpecialistRepository`; its fake `"sp1"`/`"sp2"`/`"sp3"` ids
 * would have made the real availability/booking-write endpoints reject
 * every call, since both require a real specialist `UUID`).
 * [salon]/[dashboardInsights] are real too (Phase 2, M6), via
 * [BackendDashboardRepository]/`GET /salons/mine` - plain nullable
 * snapshots, not repositories, since there's a single consumer
 * ([ai.rojan.designlab.manager.screens.dashboard.ManagerDashboardScreen])
 * reading each once per screen entry, not a list read from many call
 * sites. [dashboardInsights] failing independently (see [initialize]'s
 * doc comment) never fails the whole sync - consumers (`AIInsightCard`)
 * must treat `null` as "no data yet," not an error.
 * [salonId]/[availabilityRepository] (Final Release Validation) expose
 * what [ManagerBookingViewModel] needs to call the real computed-
 * availability API directly — [availabilityRepository] is the same
 * stateless instance the Customer flavor already uses, reused rather than
 * duplicated (salon id is a per-call parameter, not baked into it).
 *
 * [crmInsights] (Phase 7 Step 2) is populated the same way but is always
 * empty today - see its own doc comment.
 *
 * [initialize] must be called once - from
 * [ai.rojan.designlab.ManagerActivity], in a coroutine - before
 * [services]/[appointments]/[customers]/[specialists]/[salon]/
 * [dashboardInsights]/[salonId]/[availabilityRepository]/[crmInsights] have real data;
 * until then they're the empty objects/null above. That is a genuine,
 * honest gap versus the old always-ready in-memory sample data: a real
 * network round-trip cannot be instant. Whichever screen first reads them
 * may see an empty list for a moment on cold start - this composition
 * root does not attempt to solve the Compose-recomposition-on-async-load
 * problem for every existing call site (Calendar's direct, non-`remember`
 * reads in particular); that is a real, separate follow-up for whoever
 * wires this to an actual device/emulator run, not something guessed at
 * blind here.
 */
object ManagerRepositories {

    var services: ServiceRepository = EmptyServiceRepository
        private set
    var appointments: AppointmentRepository = EmptyAppointmentRepository
        private set
    var customers: CustomerRepository = EmptyCustomerRepository
        private set
    var specialists: SpecialistRepository = EmptySpecialistRepository
        private set
    var salon: ManagerSalonSummary? = null
        private set
    var dashboardInsights: ManagerDashboardInsights? = null
        private set
    var salonId: String? = null
        private set
    var availabilityRepository: AvailabilityRepository? = null
        private set

    /**
     * Manager CRM AI Consumption Layer, Phase 7 Step 2 — the registered
     * [ai.rojan.designlab.di.BackendApiContainer.managerCrmInsightProvider]'s
     * output for this salon, refreshed every [initialize]. Real since
     * Phase 7 Step 3/5: [ai.rojan.designlab.manager.domain.ai.InactiveCustomerInsightProvider]/
     * [ai.rojan.designlab.manager.domain.ai.VipCustomerInsightProvider]
     * (via [ai.rojan.designlab.manager.domain.ai.CompositeManagerCrmInsightProvider])
     * surface every [ManagerCustomer] with a matching
     * [ai.rojan.designlab.manager.domain.customer.CustomerTag] (see each
     * provider's own doc comment). Read by the Dashboard's inactive-count
     * summary and the Customer Profile's insight section (Phase 7 Steps
     * 4/7). No network call of its own - [customers]/[services]/
     * [appointments] (the last two added Phase 8 Step 1) are already-
     * synced lists passed straight into the provider.
     */
    var crmInsights: List<ManagerCrmInsight> = emptyList()
        private set

    /**
     * Resolves the caller's *active* salon (Active Salon Context &
     * Selection Flow -
     * [ai.rojan.designlab.domain.repository.ActiveSalonContextRepository],
     * already decided by
     * [ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModel]
     * before any Manager screen is reachable) against `GET
     * /api/v1/salons/mine`, and syncs real Service/Appointment/Specialist/
     * Customer data (in that order - [customerRepo] resolves service/
     * specialist names for its per-customer visit history, so both must
     * exist first), plus Dashboard Insights. Safe to call again to
     * re-sync. Deliberately does *not* fall back to `.firstOrNull()` - a
     * resolved active salon that `salons/mine` doesn't contain is a real,
     * honest failure, not something to guess past.
     *
     * Insights failing does not fail the whole call: it's fetched
     * independently of salon/service/appointment/customer/specialist
     * sync, so one genuinely-optional card being unavailable doesn't
     * block the rest of the dashboard (matching `ManagerDashboardScreen`'s
     * own "swallow the failure, show an empty state" handling).
     */
    suspend fun initialize(context: Context): Result<Unit> {
        val container = BackendApiContainerHolder.get(context)
        val activeSalonId = container.activeSalonContextRepository.observeActiveSalonId().first()
            ?: return Result.failure(IllegalStateException("No active salon selected yet"))
        val salonDto = runCatching { container.managerSalonApi.mine() }
            .getOrElse { return Result.failure(it) }
            .firstOrNull { it.id == activeSalonId }
            ?: return Result.failure(IllegalStateException("Active salon (id=$activeSalonId) not found in this account's salons"))

        val serviceRepo = BackendServiceRepository(
            serviceApi = container.serviceApi,
            serviceCategoryApi = container.serviceCategoryApi,
            managerServiceApi = container.managerServiceApi,
            salonId = salonDto.id,
        )
        val appointmentRepo = BackendAppointmentRepository(
            managerBookingApi = container.managerBookingApi,
            salonId = salonDto.id,
        )
        val specialistRepo = BackendSpecialistRepository(
            specialistApi = container.specialistApi,
            managerSpecialistApi = container.managerSpecialistApi,
            salonId = salonDto.id,
        )
        val customerRepo = BackendCustomerRepository(
            managerCustomerApi = container.managerCustomerApi,
            serviceRepository = serviceRepo,
            specialistRepository = specialistRepo,
            salonId = salonDto.id,
        )
        val dashboardRepo = BackendDashboardRepository(
            managerDashboardApi = container.managerDashboardApi,
            salonId = salonDto.id,
        )

        val serviceSync = serviceRepo.sync()
        val appointmentSync = appointmentRepo.sync()
        val specialistSync = specialistRepo.sync()
        val customerSync = customerRepo.sync()
        dashboardInsights = dashboardRepo.fetch().getOrNull()

        services = serviceRepo
        appointments = appointmentRepo
        specialists = specialistRepo
        customers = customerRepo
        salon = ManagerSalonSummary(name = salonDto.name, description = salonDto.description, active = salonDto.active)
        salonId = salonDto.id
        availabilityRepository = container.availabilityRepository
        crmInsights = container.managerCrmInsightProvider.insightsFor(
            ManagerCrmInsightContext(
                salonId = salonDto.id,
                customers = customerRepo.getAll(),
                services = serviceRepo.getAll(),
                appointments = appointmentRepo.getAll(),
            ),
        )

        return serviceSync
            .fold(onSuccess = { appointmentSync }, onFailure = { Result.failure(it) })
            .fold(onSuccess = { specialistSync }, onFailure = { Result.failure(it) })
            .fold(onSuccess = { customerSync }, onFailure = { Result.failure(it) })
    }
}
