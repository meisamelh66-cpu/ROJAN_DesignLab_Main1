package ai.rojan.designlab.presentation.specialist

import ai.rojan.designlab.domain.repository.Service
import ai.rojan.designlab.domain.repository.ServiceCategory
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.Specialist
import ai.rojan.designlab.domain.repository.SpecialistRepository
import ai.rojan.designlab.presentation.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val SALON_ID = "salon-1"
private const val SPECIALIST_ID = "specialist-1"

/** Records the exact (salonId, specialistId) pair [getAssignedServiceIds] was called with, so tests can assert the real ids reach the real endpoint rather than being dropped or substituted. */
private class FakeSpecialistRepository(
    private val specialist: Specialist,
    private val assignedServiceIds: Result<List<String>>,
) : SpecialistRepository {
    var lastAssignedServiceIdsCall: Pair<String, String>? = null
        private set

    override suspend fun getSpecialists(salonId: String): Result<List<Specialist>> = error("not used")
    override suspend fun getSpecialist(salonId: String, specialistId: String): Result<Specialist> = Result.success(specialist)
    override suspend fun getPortfolio(salonId: String, specialistId: String): Result<List<String>> = Result.success(emptyList())

    override suspend fun getAssignedServiceIds(salonId: String, specialistId: String): Result<List<String>> {
        lastAssignedServiceIdsCall = salonId to specialistId
        return assignedServiceIds
    }
}

private class FakeServiceCategoryRepository(private val categories: List<ServiceCategory>) : ServiceCategoryRepository {
    override suspend fun getCategories(salonId: String): Result<List<ServiceCategory>> = Result.success(categories)
}

private class FakeServiceRepository(private val servicesByCategory: Map<String, List<Service>>) : ServiceRepository {
    override suspend fun getServices(salonId: String, categoryId: String): Result<List<Service>> =
        Result.success(servicesByCategory[categoryId].orEmpty())

    override suspend fun getImages(salonId: String, serviceId: String): Result<List<String>> = Result.success(emptyList())
}

private fun makeSpecialist(id: String = SPECIALIST_ID) = Specialist(id, SALON_ID, "Jordan Lee", null, null)
private fun makeCategory(id: String) = ServiceCategory(id, SALON_ID, "Hair", null)
private fun makeService(id: String, categoryId: String, name: String) =
    Service(id, SALON_ID, categoryId, name, null, 60, 65.0)

/**
 * Customer Specialist -> Services Integration.
 *
 * Covers the mission's own required scenarios: the real assignment ids are
 * fetched with the correct salon/specialist ids (not inferred or dropped),
 * the eligible-services filter genuinely uses those ids (not a hard-coded
 * list), the "empty means eligible for everything" business rule is honored
 * rather than shown as zero services, and a Backend failure surfaces as a
 * real error rather than silently falling back to "show everything" (which
 * would fabricate a relationship Backend never confirmed).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SpecialistProfileViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `assigned service ids are fetched with the real salon and specialist ids`() = runTest {
        val category = makeCategory("cat-1")
        val service = makeService("service-1", "cat-1", "Haircut")
        val specialistRepository = FakeSpecialistRepository(makeSpecialist(), Result.success(listOf("service-1")))

        val viewModel = SpecialistProfileViewModel(
            salonId = SALON_ID,
            specialistId = SPECIALIST_ID,
            specialistRepository = specialistRepository,
            serviceCategoryRepository = FakeServiceCategoryRepository(listOf(category)),
            serviceRepository = FakeServiceRepository(mapOf("cat-1" to listOf(service))),
        )

        assertEquals(SALON_ID to SPECIALIST_ID, specialistRepository.lastAssignedServiceIdsCall)
        val state = viewModel.state as UiState.Success
        assertEquals(listOf(service), state.data.services)
    }

    @Test
    fun `only the backend-assigned service is shown - relationship is not hard-coded`() = runTest {
        val category = makeCategory("cat-1")
        val assigned = makeService("service-1", "cat-1", "Haircut")
        val notAssigned = makeService("service-2", "cat-1", "Manicure")
        val specialistRepository = FakeSpecialistRepository(makeSpecialist(), Result.success(listOf("service-1")))

        val viewModel = SpecialistProfileViewModel(
            salonId = SALON_ID,
            specialistId = SPECIALIST_ID,
            specialistRepository = specialistRepository,
            serviceCategoryRepository = FakeServiceCategoryRepository(listOf(category)),
            serviceRepository = FakeServiceRepository(mapOf("cat-1" to listOf(assigned, notAssigned))),
        )

        val state = viewModel.state as UiState.Success
        assertEquals(listOf(assigned), state.data.services)
        assertFalse(state.data.isAssignedToEveryService)
    }

    @Test
    fun `empty assignment list means eligible for every service, not zero services`() = runTest {
        val category = makeCategory("cat-1")
        val serviceA = makeService("service-1", "cat-1", "Haircut")
        val serviceB = makeService("service-2", "cat-1", "Manicure")

        val viewModel = SpecialistProfileViewModel(
            salonId = SALON_ID,
            specialistId = SPECIALIST_ID,
            specialistRepository = FakeSpecialistRepository(makeSpecialist(), Result.success(emptyList())),
            serviceCategoryRepository = FakeServiceCategoryRepository(listOf(category)),
            serviceRepository = FakeServiceRepository(mapOf("cat-1" to listOf(serviceA, serviceB))),
        )

        val state = viewModel.state as UiState.Success
        assertTrue(state.data.isAssignedToEveryService)
        assertEquals(listOf(serviceA, serviceB), state.data.services)
    }

    @Test
    fun `a backend failure loading the assignment shows an error, never a fabricated relationship`() = runTest {
        val category = makeCategory("cat-1")
        val service = makeService("service-1", "cat-1", "Haircut")

        val viewModel = SpecialistProfileViewModel(
            salonId = SALON_ID,
            specialistId = SPECIALIST_ID,
            specialistRepository = FakeSpecialistRepository(makeSpecialist(), Result.failure(RuntimeException("network error"))),
            serviceCategoryRepository = FakeServiceCategoryRepository(listOf(category)),
            serviceRepository = FakeServiceRepository(mapOf("cat-1" to listOf(service))),
        )

        assertTrue(viewModel.state is UiState.Error)
    }

    @Test
    fun `retry re-fetches the assignment and recovers from a prior failure`() = runTest {
        var shouldFail = true
        val category = makeCategory("cat-1")
        val service = makeService("service-1", "cat-1", "Haircut")
        val specialistRepository = object : SpecialistRepository {
            override suspend fun getSpecialists(salonId: String): Result<List<Specialist>> = error("not used")
            override suspend fun getSpecialist(salonId: String, specialistId: String): Result<Specialist> = Result.success(makeSpecialist())
            override suspend fun getPortfolio(salonId: String, specialistId: String): Result<List<String>> = Result.success(emptyList())
            override suspend fun getAssignedServiceIds(salonId: String, specialistId: String): Result<List<String>> =
                if (shouldFail) Result.failure(RuntimeException("network error")) else Result.success(listOf("service-1"))
        }

        val viewModel = SpecialistProfileViewModel(
            salonId = SALON_ID,
            specialistId = SPECIALIST_ID,
            specialistRepository = specialistRepository,
            serviceCategoryRepository = FakeServiceCategoryRepository(listOf(category)),
            serviceRepository = FakeServiceRepository(mapOf("cat-1" to listOf(service))),
        )
        assertTrue(viewModel.state is UiState.Error)

        shouldFail = false
        viewModel.retry()

        val state = viewModel.state as UiState.Success
        assertEquals(listOf(service), state.data.services)
    }
}
