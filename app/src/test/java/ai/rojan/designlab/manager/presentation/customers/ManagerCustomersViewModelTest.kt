package ai.rojan.designlab.manager.presentation.customers

import ai.rojan.designlab.data.remote.NetworkUnavailableException
import ai.rojan.designlab.domain.repository.PagedResult
import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.domain.repository.SalonCustomer
import ai.rojan.designlab.domain.repository.SalonCustomerRepository
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.presentation.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * FIX-004 (PARTIAL). [ManagerCustomersViewModel] is constructed from
 * nothing but backend-facing repository interfaces — no
 * `manager.data.ManagerRepositories`, no in-memory `ManagerCustomer`
 * type. Every state the Customers list can render is traceable to a real
 * [SalonCustomerRepository] call, never the old sample roster.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManagerCustomersViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val ownedSalon = Salon(
        id = "salon-1",
        name = "Real Salon",
        description = null,
        phone = "000",
        email = null,
        address = "addr",
    )

    private val roster = listOf(
        SalonCustomer("cust-1", "sara@example.com", "سارا محمدی"),
        SalonCustomer("cust-2", "niloofar@example.com", "نیلوفر احمدی"),
    )

    private fun viewModel(
        salonRepository: SalonRepository = FakeSalonRepository(Result.success(listOf(ownedSalon))),
        salonCustomerRepository: SalonCustomerRepository = FakeSalonCustomerRepository(Result.success(roster)),
    ) = ManagerCustomersViewModel(salonRepository, salonCustomerRepository)

    @Test
    fun `initial load resolves the owned salon and shows its real roster as Success`() = runBlocking {
        val customerRepo = FakeSalonCustomerRepository(Result.success(roster))

        val vm = viewModel(salonCustomerRepository = customerRepo)

        val state = vm.state as UiState.Success
        assertEquals(roster, state.data)
        assertEquals("salon-1", customerRepo.lastSalonId)
    }

    @Test
    fun `a salon with no matching customers shows Empty, not a fabricated list`() = runBlocking {
        val vm = viewModel(salonCustomerRepository = FakeSalonCustomerRepository(Result.success(emptyList())))

        assertEquals(UiState.Empty, vm.state)
    }

    @Test
    fun `an account that owns no salon shows Empty`() = runBlocking {
        val vm = viewModel(salonRepository = FakeSalonRepository(Result.success(emptyList())))

        assertEquals(UiState.Empty, vm.state)
    }

    @Test
    fun `a failure resolving the owned salon surfaces as Error`() = runBlocking {
        val vm = viewModel(
            salonRepository = FakeSalonRepository(Result.failure(NetworkUnavailableException(Exception("offline")))),
        )

        assertTrue(vm.state is UiState.Error)
    }

    @Test
    fun `a customer-search failure surfaces as Error, never a fake roster`() = runBlocking {
        val vm = viewModel(
            salonCustomerRepository = FakeSalonCustomerRepository(
                Result.failure(NetworkUnavailableException(Exception("offline"))),
            ),
        )

        assertTrue(vm.state is UiState.Error)
    }

    @Test
    fun `search forwards the query to the salon-scoped backend endpoint`() = runBlocking {
        val customerRepo = FakeSalonCustomerRepository(Result.success(roster))
        val vm = viewModel(salonCustomerRepository = customerRepo)

        vm.search("سارا")

        assertEquals("salon-1", customerRepo.lastSalonId)
        assertEquals("سارا", customerRepo.lastQuery)
    }
}

private class FakeSalonRepository(private val owned: Result<List<Salon>>) : SalonRepository {
    override suspend fun browseSalons(page: Int, size: Int, nameFilter: String?): Result<PagedResult<Salon>> =
        error("not used by ManagerCustomersViewModel")

    override suspend fun getSalon(salonId: String): Result<Salon> = error("not used by ManagerCustomersViewModel")

    override suspend fun myOwnedSalons(): Result<List<Salon>> = owned
}

private class FakeSalonCustomerRepository(
    private val result: Result<List<SalonCustomer>>,
) : SalonCustomerRepository {

    var lastSalonId: String? = null
        private set
    var lastQuery: String? = null
        private set

    override suspend fun searchCustomers(salonId: String, query: String?): Result<List<SalonCustomer>> {
        lastSalonId = salonId
        lastQuery = query
        return result
    }
}
