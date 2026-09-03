package ai.rojan.designlab.manager.presentation.staff

import ai.rojan.designlab.manager.domain.repository.SpecialistRepository
import ai.rojan.designlab.manager.domain.service.Service
import ai.rojan.designlab.manager.domain.specialist.Specialist
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * P1-5, Manager Specialist <-> Service Assignment: hermetic coverage of
 * [ManagerSpecialistServicesViewModel]'s Loading/Error/Ready lifecycle and
 * its per-service assign/unassign mutations, mirroring the rigor
 * established by [ai.rojan.designlab.manager.presentation.dashboard.ManagerDashboardViewModelTest]/
 * [ai.rojan.designlab.manager.presentation.services.ManagerServicesViewModelTest]/
 * [ai.rojan.designlab.manager.presentation.calendar.ManagerAppointmentDetailViewModelTest]
 * (P0-1/P0-2/P0-3) — genuine intermediate-state assertions via
 * [CompletableDeferred], real invocation-count checks, no timing-dependent
 * sleeps.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManagerSpecialistServicesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleService(id: String) = Service(
        id = id,
        name = "Service $id",
        category = "General",
        price = 100000,
        durationMinutes = 30,
        active = true,
    )

    /** Constructs a ViewModel whose initial load resolves immediately (synchronously, under [UnconfinedTestDispatcher]) to [ManagerSpecialistServicesUiState.Ready]. */
    private fun readyViewModel(
        repository: FakeSpecialistRepository,
        services: List<Service> = listOf(sampleService("s1"), sampleService("s2")),
    ): ManagerSpecialistServicesViewModel = ManagerSpecialistServicesViewModel(
        specialistId = "sp1",
        specialistRepository = { repository },
        initialize = { Result.success(Unit) },
        getServices = { services },
    )

    private fun readyState(viewModel: ManagerSpecialistServicesViewModel): ManagerSpecialistServicesUiState.Ready =
        viewModel.state as? ManagerSpecialistServicesUiState.Ready ?: error("expected Ready state, was ${viewModel.state}")

    @Test
    fun `initial load moves Loading to Ready with the real services and assigned ids`() = runTest {
        val initializeCompletion = CompletableDeferred<Result<Unit>>()
        val repository = FakeSpecialistRepository().apply { assignedServiceIdsResult = Result.success(listOf("s1")) }
        val services = listOf(sampleService("s1"), sampleService("s2"))
        val viewModel = ManagerSpecialistServicesViewModel(
            specialistId = "sp1",
            specialistRepository = { repository },
            initialize = { initializeCompletion.await() },
            getServices = { services },
        )

        assertTrue(viewModel.state is ManagerSpecialistServicesUiState.Loading)

        initializeCompletion.complete(Result.success(Unit))

        val state = readyState(viewModel)
        assertEquals(services, state.services)
        assertEquals(setOf("s1"), state.assignedServiceIds)
        assertEquals(false, state.isAssignedToEveryService)
    }

    @Test
    fun `initialize failure moves Loading to Error`() = runTest {
        val initializeCompletion = CompletableDeferred<Result<Unit>>()
        val viewModel = ManagerSpecialistServicesViewModel(
            specialistId = "sp1",
            specialistRepository = { FakeSpecialistRepository() },
            initialize = { initializeCompletion.await() },
            getServices = { emptyList() },
        )

        assertTrue(viewModel.state is ManagerSpecialistServicesUiState.Loading)

        initializeCompletion.complete(Result.failure(IllegalStateException("network down")))

        val state = viewModel.state
        assertTrue(state is ManagerSpecialistServicesUiState.Error)
        assertTrue((state as ManagerSpecialistServicesUiState.Error).message.isNotBlank())
    }

    @Test
    fun `retry after initial load failure re-invokes initialize and recovers to Ready`() = runTest {
        var initializeCallCount = 0
        var completion = CompletableDeferred<Result<Unit>>()
        val viewModel = ManagerSpecialistServicesViewModel(
            specialistId = "sp1",
            specialistRepository = { FakeSpecialistRepository() },
            initialize = { initializeCallCount++; completion.await() },
            getServices = { emptyList() },
        )

        completion.complete(Result.failure(IllegalStateException("network")))
        assertTrue(viewModel.state is ManagerSpecialistServicesUiState.Error)
        assertEquals(1, initializeCallCount)

        completion = CompletableDeferred()
        viewModel.retry()

        assertTrue(viewModel.state is ManagerSpecialistServicesUiState.Loading)
        assertEquals(2, initializeCallCount)

        completion.complete(Result.success(Unit))

        assertTrue(viewModel.state is ManagerSpecialistServicesUiState.Ready)
    }

    @Test
    fun `toggling an unassigned service calls assignService and refreshes from a real re-fetch on success`() = runTest {
        val repository = FakeSpecialistRepository().apply { assignedServiceIdsResult = Result.success(listOf("s2")) }
        val viewModel = readyViewModel(repository)

        viewModel.toggleService("s1")
        assertTrue(readyState(viewModel).pendingServiceIds.contains("s1"))
        assertEquals(1, repository.assignCallCount)

        repository.assignedServiceIdsResult = Result.success(listOf("s1", "s2"))
        repository.completeAssign("s1", Result.success(Unit))

        val state = readyState(viewModel)
        assertEquals(setOf("s1", "s2"), state.assignedServiceIds)
        assertTrue(!state.pendingServiceIds.contains("s1"))
        assertEquals(null, state.mutationError)
        assertEquals(0, repository.unassignCallCount)
    }

    @Test
    fun `toggling an unassigned service on failure clears pending, leaves assignment unchanged, and sets mutationError`() = runTest {
        val repository = FakeSpecialistRepository().apply { assignedServiceIdsResult = Result.success(listOf("s2")) }
        val viewModel = readyViewModel(repository)

        viewModel.toggleService("s1")
        assertTrue(readyState(viewModel).pendingServiceIds.contains("s1"))

        repository.completeAssign("s1", Result.failure(IllegalStateException("conflict")))

        val state = readyState(viewModel)
        assertEquals(setOf("s2"), state.assignedServiceIds) // unchanged - never optimistically mutated
        assertTrue(!state.pendingServiceIds.contains("s1"))
        assertTrue(state.mutationError?.isNotBlank() == true)
    }

    @Test
    fun `toggling a service while eligible for every service calls assignService, never unassignService, creating the first explicit restriction`() = runTest {
        val repository = FakeSpecialistRepository().apply { assignedServiceIdsResult = Result.success(emptyList()) }
        val viewModel = readyViewModel(repository)

        assertTrue(readyState(viewModel).isAssignedToEveryService)
        assertTrue(readyState(viewModel).isServiceAssigned("s1")) // displays as on - eligible for everything

        viewModel.toggleService("s1")

        assertEquals(1, repository.assignCallCount)
        assertEquals(0, repository.unassignCallCount)

        repository.assignedServiceIdsResult = Result.success(listOf("s1"))
        repository.completeAssign("s1", Result.success(Unit))

        val state = readyState(viewModel)
        assertEquals(setOf("s1"), state.assignedServiceIds)
        assertEquals(false, state.isAssignedToEveryService)
        assertTrue(state.isServiceAssigned("s1"))
        assertTrue(!state.isServiceAssigned("s2")) // now correctly excluded - the explicit whitelist took effect
        assertEquals(null, state.mutationError)
    }

    @Test
    fun `toggling off the last remaining explicit assignment returns to eligible for every service`() = runTest {
        val repository = FakeSpecialistRepository().apply { assignedServiceIdsResult = Result.success(listOf("s1")) }
        val viewModel = readyViewModel(repository)

        assertEquals(false, readyState(viewModel).isAssignedToEveryService)

        viewModel.toggleService("s1")

        assertEquals(1, repository.unassignCallCount)
        assertEquals(0, repository.assignCallCount)

        repository.assignedServiceIdsResult = Result.success(emptyList())
        repository.completeUnassign("s1", Result.success(Unit))

        val state = readyState(viewModel)
        assertEquals(emptySet<String>(), state.assignedServiceIds)
        assertTrue(state.isAssignedToEveryService)
        assertTrue(state.isServiceAssigned("s1")) // back to displaying as on - eligible for everything again
        assertTrue(state.isServiceAssigned("s2"))
    }

    @Test
    fun `toggling an assigned service calls unassignService and refreshes from a real re-fetch on success`() = runTest {
        val repository = FakeSpecialistRepository().apply { assignedServiceIdsResult = Result.success(listOf("s1", "s2")) }
        val viewModel = readyViewModel(repository)

        viewModel.toggleService("s1")
        assertTrue(readyState(viewModel).pendingServiceIds.contains("s1"))
        assertEquals(1, repository.unassignCallCount)

        repository.assignedServiceIdsResult = Result.success(listOf("s2"))
        repository.completeUnassign("s1", Result.success(Unit))

        val state = readyState(viewModel)
        assertEquals(setOf("s2"), state.assignedServiceIds)
        assertTrue(!state.pendingServiceIds.contains("s1"))
        assertEquals(0, repository.assignCallCount)
    }

    @Test
    fun `toggling an assigned service on failure clears pending, leaves assignment unchanged, and sets mutationError`() = runTest {
        val repository = FakeSpecialistRepository().apply { assignedServiceIdsResult = Result.success(listOf("s1", "s2")) }
        val viewModel = readyViewModel(repository)

        viewModel.toggleService("s1")

        repository.completeUnassign("s1", Result.failure(IllegalStateException("conflict")))

        val state = readyState(viewModel)
        assertEquals(setOf("s1", "s2"), state.assignedServiceIds) // unchanged
        assertTrue(!state.pendingServiceIds.contains("s1"))
        assertTrue(state.mutationError?.isNotBlank() == true)
    }

    @Test
    fun `toggling the same service again while its mutation is in flight does not call assignService twice`() = runTest {
        val repository = FakeSpecialistRepository().apply { assignedServiceIdsResult = Result.success(listOf("s2")) }
        val viewModel = readyViewModel(repository)

        viewModel.toggleService("s1")
        assertEquals(1, repository.assignCallCount)
        assertTrue(readyState(viewModel).pendingServiceIds.contains("s1"))

        viewModel.toggleService("s1")

        assertEquals(1, repository.assignCallCount)

        repository.completeAssign("s1", Result.success(Unit))
    }

    @Test
    fun `toggling two different services mutates them independently and concurrently`() = runTest {
        val repository = FakeSpecialistRepository().apply { assignedServiceIdsResult = Result.success(listOf("s3")) }
        val viewModel = readyViewModel(repository, services = listOf(sampleService("s1"), sampleService("s2")))

        viewModel.toggleService("s1")
        viewModel.toggleService("s2")

        assertEquals(2, repository.assignCallCount)
        val pendingBoth = readyState(viewModel).pendingServiceIds
        assertEquals(setOf("s1", "s2"), pendingBoth)

        repository.completeAssign("s1", Result.success(Unit))

        val afterFirst = readyState(viewModel)
        assertTrue(!afterFirst.pendingServiceIds.contains("s1"))
        assertTrue(afterFirst.pendingServiceIds.contains("s2")) // second mutation unaffected by the first resolving

        repository.completeAssign("s2", Result.success(Unit))

        assertTrue(!readyState(viewModel).pendingServiceIds.contains("s2"))
    }

    private class FakeSpecialistRepository : SpecialistRepository {
        var assignedServiceIdsResult: Result<List<String>> = Result.success(emptyList())

        private val assignCompletions = mutableMapOf<String, CompletableDeferred<Result<Unit>>>()
        private val unassignCompletions = mutableMapOf<String, CompletableDeferred<Result<Unit>>>()

        var assignCallCount = 0
            private set
        var unassignCallCount = 0
            private set

        fun completeAssign(serviceId: String, result: Result<Unit>) {
            assignCompletions.getOrPut(serviceId) { CompletableDeferred() }.complete(result)
        }

        fun completeUnassign(serviceId: String, result: Result<Unit>) {
            unassignCompletions.getOrPut(serviceId) { CompletableDeferred() }.complete(result)
        }

        override fun getAll(): List<Specialist> = error("not used")
        override fun getById(id: String): Specialist? = error("not used")
        override suspend fun create(specialist: Specialist): Result<Specialist> = error("not used")
        override suspend fun update(specialist: Specialist): Result<Specialist?> = error("not used")
        override suspend fun delete(id: String): Result<Boolean> = error("not used")

        override suspend fun getAssignedServiceIds(specialistId: String): Result<List<String>> = assignedServiceIdsResult

        override suspend fun assignService(specialistId: String, serviceId: String): Result<Unit> {
            assignCallCount++
            return assignCompletions.getOrPut(serviceId) { CompletableDeferred() }.await()
        }

        override suspend fun unassignService(specialistId: String, serviceId: String): Result<Unit> {
            unassignCallCount++
            return unassignCompletions.getOrPut(serviceId) { CompletableDeferred() }.await()
        }
    }
}
