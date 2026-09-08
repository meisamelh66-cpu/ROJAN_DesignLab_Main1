package ai.rojan.designlab.manager.presentation.customers

import ai.rojan.designlab.data.remote.NetworkUnavailableException
import ai.rojan.designlab.domain.repository.ManagerCustomerProfileRepository
import ai.rojan.designlab.domain.repository.PagedResult
import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.manager.domain.customer.CustomerServiceHistoryEntry
import ai.rojan.designlab.manager.domain.customer.ManagerCustomerProfile
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
 * FIX-006. [ManagerCustomerProfileViewModel] is constructed from nothing
 * but backend-facing repository interfaces — no
 * `manager.data.ManagerRepositories`, no in-memory sample record. Every
 * state the profile screen can render traces to a real
 * [ManagerCustomerProfileRepository] call.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManagerCustomerProfileViewModelTest {

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

    private val profile = ManagerCustomerProfile(
        fullName = "سارا محمدی",
        phone = "۰۹۱۲۱۲۳۴۵۶۷",
        statusLabel = "مشتری فعال · VIP",
        totalVisits = 4,
        history = listOf(CustomerServiceHistoryEntry("2026-05-01", "نوبت ایجاد شد", "", "")),
        notes = "یادداشت",
    )

    private fun viewModel(
        accountId: String = "user-1",
        salonRepository: SalonRepository = ProfileTestSalonRepository(Result.success(listOf(ownedSalon))),
        profileRepository: ManagerCustomerProfileRepository = ProfileTestProfileRepository(Result.success(profile)),
    ) = ManagerCustomerProfileViewModel(accountId, salonRepository, profileRepository)

    @Test
    fun `initial load resolves the owned salon and shows the real profile as Success`() = runBlocking {
        val repo = ProfileTestProfileRepository(Result.success(profile))

        val vm = viewModel(profileRepository = repo)

        assertEquals(UiState.Success(profile), vm.state)
        assertEquals("salon-1", repo.lastSalonId)
        assertEquals("user-1", repo.lastAccountId)
    }

    @Test
    fun `no CRM record for this account shows Empty, not fake data`() = runBlocking {
        val vm = viewModel(profileRepository = ProfileTestProfileRepository(Result.success(null)))

        assertEquals(UiState.Empty, vm.state)
    }

    @Test
    fun `an account that owns no salon shows Empty`() = runBlocking {
        val vm = viewModel(salonRepository = ProfileTestSalonRepository(Result.success(emptyList())))

        assertEquals(UiState.Empty, vm.state)
    }

    @Test
    fun `a failure resolving the owned salon surfaces as Error`() = runBlocking {
        val vm = viewModel(
            salonRepository = ProfileTestSalonRepository(Result.failure(NetworkUnavailableException(Exception("offline")))),
        )

        assertTrue(vm.state is UiState.Error)
    }

    @Test
    fun `a profile-load failure surfaces as Error, never a fake profile`() = runBlocking {
        val vm = viewModel(
            profileRepository = ProfileTestProfileRepository(Result.failure(NetworkUnavailableException(Exception("offline")))),
        )

        assertTrue(vm.state is UiState.Error)
    }
}

private class ProfileTestSalonRepository(private val owned: Result<List<Salon>>) : SalonRepository {
    override suspend fun browseSalons(page: Int, size: Int, nameFilter: String?): Result<PagedResult<Salon>> =
        error("not used by ManagerCustomerProfileViewModel")

    override suspend fun getSalon(salonId: String): Result<Salon> = error("not used by ManagerCustomerProfileViewModel")

    override suspend fun myOwnedSalons(): Result<List<Salon>> = owned
}

private class ProfileTestProfileRepository(
    private val result: Result<ManagerCustomerProfile?>,
) : ManagerCustomerProfileRepository {

    var lastSalonId: String? = null
        private set
    var lastAccountId: String? = null
        private set

    override suspend fun loadProfileByAccountId(salonId: String, accountId: String): Result<ManagerCustomerProfile?> {
        lastSalonId = salonId
        lastAccountId = accountId
        return result
    }
}
