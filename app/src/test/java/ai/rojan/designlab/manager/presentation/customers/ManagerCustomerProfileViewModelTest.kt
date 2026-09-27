package ai.rojan.designlab.manager.presentation.customers

import ai.rojan.designlab.data.remote.NetworkUnavailableException
import ai.rojan.designlab.domain.repository.CurrentUserIdentityContext
import ai.rojan.designlab.domain.repository.CurrentUserIdentityContextRepository
import ai.rojan.designlab.domain.repository.OwnedSalonAccess
import ai.rojan.designlab.domain.repository.SalonMembershipAccess
import ai.rojan.designlab.manager.domain.customer.CustomerNote
import ai.rojan.designlab.manager.domain.customer.CustomerServiceHistoryEntry
import ai.rojan.designlab.manager.domain.customer.CustomerTag
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import ai.rojan.designlab.manager.domain.repository.CustomerRepository
import ai.rojan.designlab.presentation.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Phase C (Customers ViewModel migration) — retargeted from the obsolete
 * [ai.rojan.designlab.domain.repository.SalonCustomerRepository] (which called `getCustomer`/
 * `getCustomerBookings`/`getCustomerNotes` - methods that do not exist on that interface's real,
 * current shape) onto the current [CustomerRepository]/[ManagerCustomer] the ViewModel actually
 * depends on (Phase 3A): [CustomerRepository.getById]/[CustomerRepository.getServiceHistory]/
 * [CustomerRepository.getNoteHistory]/[CustomerRepository.loadDetail].
 *
 * **P0 identity-safety contract, explicitly tested here** (mirrors, without replacing, the
 * screen-local fix already committed in
 * [ai.rojan.designlab.manager.screens.customers.ManagerCustomerProfileScreen]):
 * - the requested `customerId` is the only identity source ([the requested customerId resolves to
 *   that exact customer, never another one in the same salon]).
 * - a genuine not-found is never a substitute customer - only [UiState.Empty]
 *   ([a customer id with no match after a successful loadDetail is Empty - never a substitute customer]).
 * - a real fetch failure is always [UiState.Error], never silently reinterpreted as "not found",
 *   even when the requested customer would otherwise have resolved
 *   ([a loadDetail failure is Error, even when the customer would have resolved - never silently reinterpreted as not found]).
 * - there is no `getAll().firstOrNull()` (or any other) fallback anywhere in
 *   [ManagerCustomerProfileViewModel] - verifiable directly from its source, and unreproducible by
 *   construction here since [ProfileFakeCustomerRepository.getById] is the only lookup the
 *   ViewModel ever calls.
 *
 * Pre-release fix (Manager Salon Access Alignment, preserved): salon resolution goes through
 * [CurrentUserIdentityContextRepository]/`availableSalons()` (ownership OR membership OR
 * specialist); [ManagerCustomerProfileViewModel.noAccessibleSalon] disambiguates "no salon
 * reachable" from a genuine customer not-found.
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

    private val ownedContext = identityContext(
        ownedSalons = listOf(OwnedSalonAccess("salon-1", "سالن رویان", active = true, permissions = emptySet())),
    )
    private val customer = ManagerCustomer(
        id = "customer-1",
        name = "مشتری واقعی",
        phone = "+989120000000",
        tag = CustomerTag.REGULAR,
        loyaltyScore = 0,
        notes = null,
        lastVisit = "1404/07/05",
        totalVisits = 1,
    )
    private val historyEntry = CustomerServiceHistoryEntry(
        date = "1404/07/05",
        service = "خدمت",
        specialist = "متخصص",
        price = "۱۰۰,۰۰۰ت",
    )

    private fun viewModel(
        identityContextRepository: CurrentUserIdentityContextRepository = ProfileFakeIdentityContextRepository { Result.success(ownedContext) },
        customerRepository: CustomerRepository = ProfileFakeCustomerRepository(
            customers = mapOf(customer.id to customer),
            history = listOf(historyEntry),
        ),
        customerId: String = "customer-1",
    ) = ManagerCustomerProfileViewModel(
        customerRepository = customerRepository,
        currentUserIdentityContextRepository = identityContextRepository,
        customerId = customerId,
    )

    @Test
    fun `a real customer with real history loads into Success`() = runBlocking {
        val viewModel = viewModel()

        val state = viewModel.state as UiState.Success
        assertEquals(customer, state.data.customer)
        assertEquals(1, state.data.history.size)
        assertFalse(viewModel.noAccessibleSalon)
    }

    @Test
    fun `the requested customerId resolves to that exact customer, never another one in the same salon`() = runBlocking {
        val other = customer.copy(id = "customer-2", name = "مشتری دیگر")
        val viewModel = viewModel(
            customerRepository = ProfileFakeCustomerRepository(
                customers = mapOf(customer.id to customer, other.id to other),
                history = listOf(historyEntry),
            ),
            customerId = customer.id,
        )

        val state = viewModel.state as UiState.Success
        assertEquals(customer.id, state.data.customer.id)
        assertTrue(state.data.customer.id != other.id)
    }

    @Test
    fun `a customer id with no match after a successful loadDetail is Empty - never a substitute customer`() = runBlocking {
        val other = customer.copy(id = "customer-2", name = "مشتری دیگر")
        val viewModel = viewModel(
            customerRepository = ProfileFakeCustomerRepository(
                // "customer-1" is genuinely absent - only a *different* customer exists in this salon.
                customers = mapOf(other.id to other),
            ),
            customerId = "customer-1",
        )

        assertEquals(UiState.Empty, viewModel.state)
        assertFalse("a genuine not-found is not a no-access state", viewModel.noAccessibleSalon)
    }

    @Test
    fun `a loadDetail failure is Error, even when the customer would have resolved - never silently reinterpreted as not found`() = runBlocking {
        val viewModel = viewModel(
            customerRepository = ProfileFakeCustomerRepository(
                customers = mapOf(customer.id to customer), // would resolve if getById were ever reached
                loadDetailResult = Result.failure(NetworkUnavailableException(Exception("offline"))),
            ),
        )

        assertTrue(viewModel.state is UiState.Error)
    }

    @Test
    fun `no accessible salon at all is Empty with noAccessibleSalon true`() = runBlocking {
        val viewModel = viewModel(identityContextRepository = ProfileFakeIdentityContextRepository { Result.success(identityContext()) })

        assertEquals(UiState.Empty, viewModel.state)
        assertTrue("no owned/member/specialist salon must set noAccessibleSalon", viewModel.noAccessibleSalon)
    }

    @Test
    fun `a manager with membership-only access resolves the customer to Success, not a false no-salon state`() = runBlocking {
        val membershipOnly = identityContext(
            memberships = listOf(SalonMembershipAccess("membership-1", "salon-1", "سالن رویان", active = true, role = "MANAGER", permissions = emptySet())),
        )
        val viewModel = viewModel(identityContextRepository = ProfileFakeIdentityContextRepository { Result.success(membershipOnly) })

        val state = viewModel.state
        assertTrue("expected Success for membership-only access, was $state", state is UiState.Success)
        assertFalse(viewModel.noAccessibleSalon)
    }

    @Test
    fun `a network failure loading identity context shows Error state`() = runBlocking {
        val viewModel = viewModel(identityContextRepository = ProfileFakeIdentityContextRepository { Result.failure(NetworkUnavailableException(Exception("offline"))) })

        assertTrue(viewModel.state is UiState.Error)
    }
}

private fun identityContext(
    ownedSalons: List<OwnedSalonAccess> = emptyList(),
    memberships: List<SalonMembershipAccess> = emptyList(),
) = CurrentUserIdentityContext(
    userId = "user-1",
    phoneNumber = null,
    email = null,
    fullName = "مدیر تست",
    globalRole = "CUSTOMER",
    ownedSalons = ownedSalons,
    memberships = memberships,
    specialistLinks = emptyList(),
)

private class ProfileFakeIdentityContextRepository(
    private val result: suspend () -> Result<CurrentUserIdentityContext>,
) : CurrentUserIdentityContextRepository {
    override suspend fun getCurrentUserIdentityContext(): Result<CurrentUserIdentityContext> = result()
}

/**
 * [customers] models the salon's real, already-synced identity cache - [getById] is the ONLY
 * lookup [ManagerCustomerProfileViewModel] ever calls, so there is no code path here (or in the
 * ViewModel) that could substitute a different entry. [loadDetailResult] lets a test force a real
 * fetch failure independently of whether the requested id would otherwise resolve.
 */
private class ProfileFakeCustomerRepository(
    private val customers: Map<String, ManagerCustomer> = emptyMap(),
    private val history: List<CustomerServiceHistoryEntry> = emptyList(),
    private val notes: List<CustomerNote> = emptyList(),
    private val loadDetailResult: Result<Unit> = Result.success(Unit),
) : CustomerRepository {
    override fun getAll(): List<ManagerCustomer> = customers.values.toList()
    override fun getById(id: String): ManagerCustomer? = customers[id]
    override fun search(query: String): List<ManagerCustomer> = customers.values.toList()
    override suspend fun create(customer: ManagerCustomer): Result<ManagerCustomer> = error("not used by these tests")
    override suspend fun update(customer: ManagerCustomer): Result<ManagerCustomer?> = error("not used by these tests")
    override fun getServiceHistory(customerId: String): List<CustomerServiceHistoryEntry> = history
    override fun getNoteHistory(customerId: String): List<CustomerNote> = notes
    override suspend fun loadDetail(customerId: String): Result<Unit> = loadDetailResult
}
