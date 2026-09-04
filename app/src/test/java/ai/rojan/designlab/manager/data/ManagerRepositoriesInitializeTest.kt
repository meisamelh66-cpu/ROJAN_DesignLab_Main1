package ai.rojan.designlab.manager.data

import ai.rojan.designlab.domain.repository.AvailabilityRepository
import ai.rojan.designlab.domain.repository.TimeSlot
import ai.rojan.designlab.manager.domain.ai.ManagerCrmInsight
import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus
import ai.rojan.designlab.manager.domain.customer.CustomerNote
import ai.rojan.designlab.manager.domain.customer.CustomerServiceHistoryEntry
import ai.rojan.designlab.manager.domain.customer.CustomerTag
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import ai.rojan.designlab.manager.domain.dashboard.ManagerDashboardInsights
import ai.rojan.designlab.manager.domain.dashboard.ManagerSalonSummary
import ai.rojan.designlab.manager.domain.repository.AppointmentRepository
import ai.rojan.designlab.manager.domain.repository.CustomerRepository
import ai.rojan.designlab.manager.domain.repository.ServiceRepository
import ai.rojan.designlab.manager.domain.repository.SpecialistRepository
import ai.rojan.designlab.manager.domain.service.Service
import ai.rojan.designlab.manager.domain.specialist.Specialist
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Sprint 5B-5 — [ManagerRepositories.initialize] in-flight de-dup (5B5-2)
 * and last-known-good preservation on a failed re-sync (5B5-1).
 *
 * Hermetic: no Android context, no real backend — [ManagerRepositories.initializeWith]
 * takes a substituted loader. The shared init runs on [Dispatchers.Main]
 * (a [StandardTestDispatcher]), driven by the test scheduler.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManagerRepositoriesInitializeTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        ManagerRepositories.resetForTest()
    }

    @After
    fun tearDown() {
        ManagerRepositories.resetForTest()
        Dispatchers.resetMain()
    }

    // ---- fakes -------------------------------------------------------

    private class FakeServiceRepo(private val items: List<Service>) : ServiceRepository {
        override fun getAll() = items
        override fun getById(id: String) = items.find { it.id == id }
        override suspend fun create(service: Service) = error("unused")
        override suspend fun update(service: Service) = error("unused")
        override suspend fun delete(id: String) = error("unused")
        override fun getCategoryNames() = emptyList<String>()
    }

    private class FakeAppointmentRepo(private val items: List<Appointment>) : AppointmentRepository {
        override fun getAll() = items
        override fun getById(id: String) = items.find { it.id == id }
        override fun getByCustomerId(customerId: String) = items.filter { it.customerId == customerId }
        override suspend fun create(appointment: Appointment) = error("unused")
        override fun update(appointment: Appointment): Appointment? = null
        override fun updateStatus(id: String, status: AppointmentStatus): Appointment? = null
        override fun cancel(id: String): Appointment? = null
        override suspend fun createForCustomer(
            customerId: String,
            serviceId: String,
            specialistId: String,
            startTime: String,
            notes: String?,
        ) = error("unused")
        override suspend fun confirm(id: String) = error("unused")
        override suspend fun complete(id: String) = error("unused")
    }

    private class FakeSpecialistRepo(private val items: List<Specialist>) : SpecialistRepository {
        override fun getAll() = items
        override fun getById(id: String) = items.find { it.id == id }
        override suspend fun create(specialist: Specialist) = error("unused")
        override suspend fun update(specialist: Specialist) = error("unused")
        override suspend fun delete(id: String) = error("unused")
    }

    private class FakeCustomerRepo(private val items: List<ManagerCustomer>) : CustomerRepository {
        override fun getAll() = items
        override fun getById(id: String) = items.find { it.id == id }
        override fun search(query: String) = items
        override suspend fun create(customer: ManagerCustomer) = error("unused")
        override suspend fun update(customer: ManagerCustomer) = error("unused")
        override fun getServiceHistory(customerId: String) = emptyList<CustomerServiceHistoryEntry>()
        override fun getNoteHistory(customerId: String) = emptyList<CustomerNote>()
        override suspend fun loadDetail(customerId: String) = Result.success(Unit)
    }

    private object FakeAvailability : AvailabilityRepository {
        override suspend fun getAvailableSlots(
            salonId: String,
            specialistId: String,
            serviceId: String,
            date: String,
            slotIntervalMinutes: Int,
        ): Result<List<TimeSlot>> = Result.success(emptyList())
    }

    // ---- builders --------------------------------------------------

    private fun service(id: String) = Service(id, "svc-$id", "cat", 0L, 30, true)
    private fun appointment(id: String) =
        Appointment(id, "c1", "s1", "sp1", "2026/09/01", "10:00", AppointmentStatus.CONFIRMED)
    private fun specialist(id: String) = Specialist(id, "sp-$id", emptyList(), "", 0.0, true)
    private fun customer(id: String) =
        ManagerCustomer(id, "cust-$id", "0", CustomerTag.REGULAR, 0, null, "2026/09/01", 0)
    private fun insights(message: String) = ManagerDashboardInsights(0.0, 0, message)

    private fun initData(
        servicesSync: Result<Unit> = Result.success(Unit),
        appointmentsSync: Result<Unit> = Result.success(Unit),
        specialistsSync: Result<Unit> = Result.success(Unit),
        customersSync: Result<Unit> = Result.success(Unit),
        services: List<Service> = listOf(service("new")),
        appointments: List<Appointment> = listOf(appointment("new")),
        specialists: List<Specialist> = listOf(specialist("new")),
        customers: List<ManagerCustomer> = listOf(customer("new")),
        dashboard: Result<ManagerDashboardInsights> = Result.success(insights("new")),
        crm: Result<List<ManagerCrmInsight>> = Result.success(emptyList()),
    ) = ManagerInitData(
        services = RepoSync(FakeServiceRepo(services), servicesSync),
        appointments = RepoSync(FakeAppointmentRepo(appointments), appointmentsSync),
        specialists = RepoSync(FakeSpecialistRepo(specialists), specialistsSync),
        customers = RepoSync(FakeCustomerRepo(customers), customersSync),
        salon = ManagerSalonSummary("salon-1", "Salon", null, "0", null, "addr", null, null, true, null, null),
        salonId = "salon-1",
        availabilityRepository = FakeAvailability,
        dashboardInsights = dashboard,
        crmInsights = crm,
    )

    /** A loader whose completion is controlled by the test, counting invocations. */
    private class GatedLoader {
        var callCount = 0
            private set
        private var gate: CompletableDeferred<Result<ManagerInitData>>? = null

        val load: suspend () -> Result<ManagerInitData> = {
            callCount++
            CompletableDeferred<Result<ManagerInitData>>().also { gate = it }.await()
        }

        fun release(result: Result<ManagerInitData>) {
            gate!!.complete(result)
        }
    }

    // ---- T1: sequential fresh init — NO TTL ----------------------

    @Test
    fun `each completed initialize starts a fresh sync (no TTL)`() = runTest(dispatcher) {
        var calls = 0
        val load: suspend () -> Result<ManagerInitData> = { calls++; Result.success(initData()) }

        assertTrue(ManagerRepositories.initializeWith(load).isSuccess)
        assertEquals(1, calls)

        // second call, AFTER the first completed → a brand-new sync
        assertTrue(ManagerRepositories.initializeWith(load).isSuccess)
        assertEquals(2, calls)
    }

    // ---- T2: concurrent init — exactly one sync -----------------

    @Test
    fun `concurrent initialize calls share one underlying sync and the same result`() = runTest(dispatcher) {
        val loader = GatedLoader()

        val c1 = async { ManagerRepositories.initializeWith(loader.load) }
        val c2 = async { ManagerRepositories.initializeWith(loader.load) }
        advanceUntilIdle()

        assertEquals("only one loader invocation", 1, loader.callCount)

        loader.release(Result.success(initData()))
        advanceUntilIdle()

        assertTrue(c1.await().isSuccess)
        assertTrue(c2.await().isSuccess)
        assertEquals(1, loader.callCount)
    }

    // ---- T3: failed init is retriable --------------------------

    @Test
    fun `a failed initialization can be retried by a later call`() = runTest(dispatcher) {
        val loader = GatedLoader()

        val c1 = async { ManagerRepositories.initializeWith(loader.load) }
        val c2 = async { ManagerRepositories.initializeWith(loader.load) }
        advanceUntilIdle()
        assertEquals(1, loader.callCount)

        loader.release(Result.failure(IOException("boom")))
        advanceUntilIdle()
        assertTrue(c1.await().isFailure)
        assertTrue(c2.await().isFailure)

        // a NEW call starts a fresh attempt — the failure did not poison anything
        val c3 = async { ManagerRepositories.initializeWith(loader.load) }
        advanceUntilIdle()
        assertEquals(2, loader.callCount)

        loader.release(Result.success(initData()))
        advanceUntilIdle()
        assertTrue(c3.await().isSuccess)
    }

    // ---- T4: failed appointment sync preserves previous repo ----

    @Test
    fun `a failed appointment sync keeps the previously synced appointments`() = runTest(dispatcher) {
        ManagerRepositories.initializeWith { Result.success(initData(appointments = listOf(appointment("OLD")))) }
        assertEquals(listOf("OLD"), ManagerRepositories.appointments.getAll().map { it.id })

        val result = ManagerRepositories.initializeWith {
            Result.success(
                initData(
                    appointmentsSync = Result.failure(IOException("appt down")),
                    appointments = listOf(appointment("NEW")),
                ),
            )
        }

        assertTrue(result.isFailure)
        assertEquals(listOf("OLD"), ManagerRepositories.appointments.getAll().map { it.id })
    }

    // ---- T5: partial success -----------------------------------

    @Test
    fun `partial success updates the healthy categories and keeps the failed one`() = runTest(dispatcher) {
        ManagerRepositories.initializeWith {
            Result.success(initData(services = listOf(service("OLD_S")), appointments = listOf(appointment("OLD_A"))))
        }

        val result = ManagerRepositories.initializeWith {
            Result.success(
                initData(
                    appointmentsSync = Result.failure(IOException("appt down")),
                    services = listOf(service("NEW_S")),
                    appointments = listOf(appointment("NEW_A")),
                    specialists = listOf(specialist("NEW_SP")),
                    customers = listOf(customer("NEW_C")),
                ),
            )
        }

        assertTrue(result.isFailure)
        assertEquals(listOf("NEW_S"), ManagerRepositories.services.getAll().map { it.id })
        assertEquals(listOf("NEW_SP"), ManagerRepositories.specialists.getAll().map { it.id })
        assertEquals(listOf("NEW_C"), ManagerRepositories.customers.getAll().map { it.id })
        assertEquals(listOf("OLD_A"), ManagerRepositories.appointments.getAll().map { it.id })
    }

    // ---- T6: first initialization with a failure ---------------

    @Test
    fun `first initialization leaves the failed category empty but populates the rest`() = runTest(dispatcher) {
        val result = ManagerRepositories.initializeWith {
            Result.success(
                initData(
                    appointmentsSync = Result.failure(IOException("appt down")),
                    services = listOf(service("NEW_S")),
                    appointments = listOf(appointment("NEW_A")),
                ),
            )
        }

        assertTrue(result.isFailure)
        assertTrue(ManagerRepositories.appointments.getAll().isEmpty())
        assertEquals(listOf("NEW_S"), ManagerRepositories.services.getAll().map { it.id })
    }

    // ---- T7: dashboard failure preserves last-known-good --------

    @Test
    fun `a failed dashboard fetch keeps the previous insights and does not fail the sync`() = runTest(dispatcher) {
        ManagerRepositories.initializeWith { Result.success(initData(dashboard = Result.success(insights("GOOD")))) }
        assertEquals("GOOD", ManagerRepositories.dashboardInsights?.topRecommendationMessage)

        val result = ManagerRepositories.initializeWith {
            Result.success(initData(dashboard = Result.failure(IOException("insights down"))))
        }

        assertTrue("dashboard failure is non-fatal, like before", result.isSuccess)
        assertEquals("GOOD", ManagerRepositories.dashboardInsights?.topRecommendationMessage)
    }

    // ---- T8: joining caller cancellation does not cancel shared work ----

    @Test
    fun `a joining caller cancelling its await does not cancel the shared initialization`() = runTest(dispatcher) {
        val loader = GatedLoader()

        val leader = async { ManagerRepositories.initializeWith(loader.load) }
        advanceUntilIdle()
        val follower = async { ManagerRepositories.initializeWith(loader.load) }
        advanceUntilIdle()
        assertEquals(1, loader.callCount)

        follower.cancel()
        advanceUntilIdle()

        loader.release(Result.success(initData()))
        advanceUntilIdle()

        assertTrue("leader still completes", leader.await().isSuccess)
        assertTrue(follower.isCancelled)
        assertEquals("no duplicate sync", 1, loader.callCount)
    }

    @Test
    fun `the caller that started the init cancelling its await does not cancel the shared work`() = runTest(dispatcher) {
        val loader = GatedLoader()

        val leader = async { ManagerRepositories.initializeWith(loader.load) }
        advanceUntilIdle()
        val follower = async { ManagerRepositories.initializeWith(loader.load) }
        advanceUntilIdle()
        assertEquals(1, loader.callCount)

        leader.cancel()
        advanceUntilIdle()

        loader.release(Result.success(initData(services = listOf(service("SHARED")))))
        advanceUntilIdle()

        assertTrue("follower still completes", follower.await().isSuccess)
        assertTrue(leader.isCancelled)
        assertEquals(1, loader.callCount)
        assertEquals(listOf("SHARED"), ManagerRepositories.services.getAll().map { it.id })
    }

    // ---- extra: a clean successful init populates everything ----

    @Test
    fun `a clean successful init populates every category and returns success`() = runTest(dispatcher) {
        val result = ManagerRepositories.initializeWith {
            Result.success(
                initData(
                    services = listOf(service("S")),
                    appointments = listOf(appointment("A")),
                    specialists = listOf(specialist("SP")),
                    customers = listOf(customer("C")),
                ),
            )
        }

        assertTrue(result.isSuccess)
        assertEquals(listOf("S"), ManagerRepositories.services.getAll().map { it.id })
        assertEquals(listOf("A"), ManagerRepositories.appointments.getAll().map { it.id })
        assertEquals(listOf("SP"), ManagerRepositories.specialists.getAll().map { it.id })
        assertEquals(listOf("C"), ManagerRepositories.customers.getAll().map { it.id })
        assertEquals("salon-1", ManagerRepositories.salonId)
        assertEquals("new", ManagerRepositories.dashboardInsights?.topRecommendationMessage)
        assertFalse(ManagerRepositories.salon.value == null)
    }

    @Test
    fun `a salon-resolution failure returns failure and touches nothing`() = runTest(dispatcher) {
        val result = ManagerRepositories.initializeWith { Result.failure(IllegalStateException("no active salon")) }

        assertTrue(result.isFailure)
        assertTrue(ManagerRepositories.services.getAll().isEmpty())
        assertTrue(ManagerRepositories.appointments.getAll().isEmpty())
        assertNull(ManagerRepositories.salonId)
        assertNull(ManagerRepositories.salon.value)
    }
}
