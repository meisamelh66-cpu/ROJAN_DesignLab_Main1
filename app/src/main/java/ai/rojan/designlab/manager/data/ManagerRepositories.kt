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
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    override suspend fun confirm(id: String): Result<Appointment> =
        Result.failure(IllegalStateException("ManagerRepositories.initialize() has not completed yet"))
    override suspend fun complete(id: String): Result<Appointment> =
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
 * A freshly-constructed candidate repository plus the outcome of its
 * `sync()`. [ManagerRepositories.initialize] swaps [repository] into the
 * singleton **only when [syncResult] succeeded** (5B5-1) — a failed sync
 * leaves the previous known-good repository in place.
 */
internal class RepoSync<T>(val repository: T, val syncResult: Result<Unit>)

/**
 * Everything one [ManagerRepositories.initialize] pass fetched from the
 * backend, before the last-known-good merge is applied.
 */
internal class ManagerInitData(
    val services: RepoSync<ServiceRepository>,
    val appointments: RepoSync<AppointmentRepository>,
    val specialists: RepoSync<SpecialistRepository>,
    val customers: RepoSync<CustomerRepository>,
    val salon: ManagerSalonSummary,
    val salonId: String,
    val availabilityRepository: AvailabilityRepository,
    val dashboardInsights: Result<ManagerDashboardInsights>,
    val crmInsights: Result<List<ManagerCrmInsight>>,
)

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
    private val _salon = MutableStateFlow<ManagerSalonSummary?>(null)

    /**
     * Active Salon Context & Selection Flow — Manager Dashboard Active
     * Salon Fix: observable so [ai.rojan.designlab.manager.screens.dashboard.ManagerDashboardScreen]'s
     * salon-identity card actually recomposes once [initialize] resolves
     * it, instead of reading a plain `var` once and never again (the
     * root cause identified in `ROJAN_Active_Salon_Context_Root_Cause_Report_v1.md`).
     */
    val salon: StateFlow<ManagerSalonSummary?> = _salon.asStateFlow()

    var dashboardInsights: ManagerDashboardInsights? = null
        private set
    var salonId: String? = null
        private set
    var availabilityRepository: AvailabilityRepository? = null
        private set

    /**
     * Central Salon Management — Salon Media UI: lets
     * [ai.rojan.designlab.manager.presentation.settings.ManagerSalonMediaViewModel]
     * push the fresh [ManagerSalonSummary] a successful
     * `assignIdentity` call already returned back into this shared
     * singleton, so the Dashboard's own salon identity card reflects a
     * newly-assigned logo/cover immediately, without a full [initialize]
     * re-sync. No network call of its own - purely an in-memory update of
     * data the caller already has.
     */
    fun updateSalon(updated: ManagerSalonSummary) {
        _salon.value = updated
    }

    /**
     * Manager Dashboard Active Salon Fix: clears the cached active-salon
     * snapshot on logout ([ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModel.clearSession])
     * so a later [initialize] for a different account/salon is never
     * preceded by a visible frame of the previous session's stale salon
     * identity. Scoped to salon/salonId only, per this fix's approved
     * scope — services/appointments/customers/specialists/etc. are
     * re-synced wholesale by the next [initialize] call regardless.
     */
    fun clearActiveSalon() {
        _salon.value = null
        salonId = null
    }

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

    // 5B5-2 — in-flight de-dup. The shared init runs on [initScope] (a
    // process-lifetime scope, NOT tied to any one caller's coroutine), so a
    // caller navigating away and cancelling its own `await()` never cancels
    // the shared work for the other callers. [initMutex] guards only the
    // check-or-start of [inFlight]; the sync + merge run outside the mutex.
    private val initMutex = Mutex()
    private var initScope: CoroutineScope? = null
    private var inFlight: Deferred<Result<Unit>>? = null

    private fun scope(): CoroutineScope =
        initScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { initScope = it }

    /**
     * Resolves the caller's *active* salon (Active Salon Context &
     * Selection Flow -
     * [ai.rojan.designlab.domain.repository.ActiveSalonContextRepository],
     * already decided by
     * [ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModel]
     * before any Manager screen is reachable) via `GET
     * /api/v1/salons/{salonId}` ([ai.rojan.designlab.data.remote.SalonApi.getSalon]),
     * and syncs real Service/Appointment/Specialist/Customer data (in that
     * order - [customerRepo] resolves service/specialist names for its
     * per-customer visit history, so both must exist first), plus
     * Dashboard Insights.
     *
     * **Freshness contract (unchanged):** every call performs a fresh
     * backend sync. The *only* reason a call does not start a new sync is
     * that an equivalent initialization is already **in flight** and can be
     * joined (5B5-2) — there is no TTL. A call made after a previous one
     * has completed (success or failure) always starts a new sync.
     *
     * **Last-known-good (5B5-1):** a category whose `sync()` fails does not
     * replace the previously-synced repository — a transient failure in one
     * category never blanks another category's good data. [dashboardInsights]
     * / [crmInsights] follow the same rule (a failed fetch keeps the last
     * good value rather than nulling it). The overall [Result] still reports
     * the first sync failure, so a future caller can see initialization
     * failed.
     *
     * Deliberately **not** `GET /api/v1/salons/mine` (RBAC compatibility
     * fix - that endpoint is owner-only, `salonRepository.findByOwnerId`
     * server-side, so it returns empty for a genuine `SalonMembership`-based
     * MANAGER and this whole composition root would fail to initialize for
     * every real manager, independent of any per-endpoint permission fix).
     * `GET /salons/{salonId}` only requires the salon to exist - not
     * ownership - which is correct here because [activeSalonId] was
     * already resolved from real, authorized salon access (owner,
     * membership, or specialist link) one layer up.
     */
    suspend fun initialize(context: Context): Result<Unit> =
        joinOrStart { loadFromBackend(context) }

    /** Test entry: same in-flight de-dup + last-known-good merge, with a substituted loader. */
    @VisibleForTesting
    internal suspend fun initializeWith(load: suspend () -> Result<ManagerInitData>): Result<Unit> =
        joinOrStart(load)

    private suspend fun joinOrStart(load: suspend () -> Result<ManagerInitData>): Result<Unit> {
        val deferred = initMutex.withLock {
            inFlight?.takeIf { it.isActive }
                ?: scope().async { runInitialize(load) }.also { inFlight = it }
        }
        return deferred.await()
    }

    private suspend fun runInitialize(load: suspend () -> Result<ManagerInitData>): Result<Unit> {
        val data = load().getOrElse { return Result.failure(it) }

        // No suspension points below — in production this runs on
        // Dispatchers.Main.immediate (initScope), so it cannot interleave
        // with a concurrent reader or a second init's own merge.
        if (data.services.syncResult.isSuccess) services = data.services.repository
        if (data.appointments.syncResult.isSuccess) appointments = data.appointments.repository
        if (data.specialists.syncResult.isSuccess) specialists = data.specialists.repository
        if (data.customers.syncResult.isSuccess) customers = data.customers.repository

        // The salon fetch already succeeded (otherwise `data` would be a
        // failure) — its details are valid, so always apply them.
        _salon.value = data.salon
        salonId = data.salonId
        availabilityRepository = data.availabilityRepository

        data.dashboardInsights.onSuccess { dashboardInsights = it }

        // crmInsights is derived from customers + services + appointments;
        // only refresh it when all three inputs refreshed, so it never
        // reflects a partial mix (keep the last-known-good otherwise).
        if (data.services.syncResult.isSuccess &&
            data.appointments.syncResult.isSuccess &&
            data.customers.syncResult.isSuccess
        ) {
            data.crmInsights.onSuccess { crmInsights = it }
        }

        return data.services.syncResult
            .fold(onSuccess = { data.appointments.syncResult }, onFailure = { Result.failure(it) })
            .fold(onSuccess = { data.specialists.syncResult }, onFailure = { Result.failure(it) })
            .fold(onSuccess = { data.customers.syncResult }, onFailure = { Result.failure(it) })
    }

    /** The one network-touching step — resolves the active salon and syncs every category. */
    private suspend fun loadFromBackend(context: Context): Result<ManagerInitData> {
        val container = BackendApiContainerHolder.get(context)
        val activeSalonId = container.activeSalonContextRepository.observeActiveSalonId().first()
            ?: return Result.failure(IllegalStateException("No active salon selected yet"))
        val salonDto = runCatching { container.salonApi.getSalon(activeSalonId) }
            .getOrElse { return Result.failure(it) }

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

        // Sequential, same order as before (customerRepo resolves service/
        // specialist names, so both must sync first).
        val serviceSync = serviceRepo.sync()
        val appointmentSync = appointmentRepo.sync()
        val specialistSync = specialistRepo.sync()
        val customerSync = customerRepo.sync()
        val dashboardResult = dashboardRepo.fetch()
        val crmResult = runCatching {
            container.managerCrmInsightProvider.insightsFor(
                ManagerCrmInsightContext(
                    salonId = salonDto.id,
                    customers = customerRepo.getAll(),
                    services = serviceRepo.getAll(),
                    appointments = appointmentRepo.getAll(),
                ),
            )
        }

        return Result.success(
            ManagerInitData(
                services = RepoSync(serviceRepo, serviceSync),
                appointments = RepoSync(appointmentRepo, appointmentSync),
                specialists = RepoSync(specialistRepo, specialistSync),
                customers = RepoSync(customerRepo, customerSync),
                salon = ManagerSalonSummary(
                    id = salonDto.id,
                    name = salonDto.name,
                    description = salonDto.description,
                    phone = salonDto.phone,
                    email = salonDto.email,
                    address = salonDto.address,
                    latitude = salonDto.latitude,
                    longitude = salonDto.longitude,
                    active = salonDto.active,
                    logoUrl = salonDto.logoUrl,
                    coverImageUrl = salonDto.coverImageUrl,
                ),
                salonId = salonDto.id,
                availabilityRepository = container.availabilityRepository,
                dashboardInsights = dashboardResult,
                crmInsights = crmResult,
            ),
        )
    }

    /** Resets every field and the in-flight state to a clean slate. Test-only. */
    @VisibleForTesting
    internal fun resetForTest() {
        initScope?.cancel()
        initScope = null
        inFlight = null
        services = EmptyServiceRepository
        appointments = EmptyAppointmentRepository
        specialists = EmptySpecialistRepository
        customers = EmptyCustomerRepository
        _salon.value = null
        dashboardInsights = null
        salonId = null
        availabilityRepository = null
        crmInsights = emptyList()
    }
}
