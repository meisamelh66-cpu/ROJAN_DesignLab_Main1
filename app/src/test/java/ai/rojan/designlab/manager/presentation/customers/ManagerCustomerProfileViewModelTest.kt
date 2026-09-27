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
 *
 * Phase F2 Timing Fix (initialization-race audit): [ManagerCustomerProfileViewModel] now takes
 * `customerRepositoryProvider: () -> CustomerRepository` instead of a captured [CustomerRepository]
 * instance. Every existing test below wraps its fake in a constant-returning lambda (`{ repository
 * }`), which is behaviorally identical to before - only [TEST B/stale-repository-recovery] test
 * actually varies what the provider returns between calls, since that's the exact scenario the fix
 * addresses.
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
        customerRepositoryProvider: () -> CustomerRepository = {
            ProfileFakeCustomerRepository(
                customers = mapOf(customer.id to customer),
                history = listOf(historyEntry),
            )
        },
        customerId: String = "customer-1",
    ) = ManagerCustomerProfileViewModel(
        customerRepositoryProvider = customerRepositoryProvider,
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
        val repository = ProfileFakeCustomerRepository(
            customers = mapOf(customer.id to customer, other.id to other),
            history = listOf(historyEntry),
        )
        val viewModel = viewModel(
            customerRepositoryProvider = { repository },
            customerId = customer.id,
        )

        val state = viewModel.state as UiState.Success
        assertEquals(customer.id, state.data.customer.id)
        assertTrue(state.data.customer.id != other.id)
    }

    @Test
    fun `a customer id with no match after a successful loadDetail is Empty - never a substitute customer`() = runBlocking {
        val other = customer.copy(id = "customer-2", name = "مشتری دیگر")
        val repository = ProfileFakeCustomerRepository(
            // "customer-1" is genuinely absent - only a *different* customer exists in this salon.
            customers = mapOf(other.id to other),
        )
        val viewModel = viewModel(
            customerRepositoryProvider = { repository },
            customerId = "customer-1",
        )

        assertEquals(UiState.Empty, viewModel.state)
        assertFalse("a genuine not-found is not a no-access state", viewModel.noAccessibleSalon)
    }

    @Test
    fun `a loadDetail failure is Error, even when the customer would have resolved - never silently reinterpreted as not found`() = runBlocking {
        val repository = ProfileFakeCustomerRepository(
            customers = mapOf(customer.id to customer), // would resolve if getById were ever reached
            loadDetailResult = Result.failure(NetworkUnavailableException(Exception("offline"))),
        )
        val viewModel = viewModel(customerRepositoryProvider = { repository })

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

    /**
     * Phase F1-B Refresh Fix (Phase E audit finding) regression coverage. The screen's own fix
     * (a `LifecycleResumeEffect` calling [ManagerCustomerProfileViewModel.retry] on return from
     * Edit) is not unit-testable without Compose/Robolectric infrastructure this phase does not
     * introduce; what IS directly testable, and is the real mechanism that fix depends on, is that
     * [ManagerCustomerProfileViewModel.retry] correctly re-reads whatever the injected
     * [ai.rojan.designlab.manager.domain.repository.CustomerRepository] currently returns — exactly
     * as it would after [ai.rojan.designlab.manager.data.BackendCustomerRepository.update] mutates
     * its cache in place. [ProfileFakeCustomerRepository.setCustomer] models that same in-place
     * mutation (not a new repository instance), matching production shape.
     */
    @Test
    fun `retry reflects a customer update that happened after the initial load, the same way returning from Edit does`() = runBlocking {
        val repository = ProfileFakeCustomerRepository(
            customers = mapOf(customer.id to customer),
            history = listOf(),
        )
        val viewModel = viewModel(customerRepositoryProvider = { repository })
        assertEquals(customer.name, (viewModel.state as UiState.Success).data.customer.name)

        val updated = customer.copy(name = "نام ویرایش‌شده", phone = "+989121111111")
        repository.setCustomer(updated)
        viewModel.retry()

        val refreshedState = viewModel.state as UiState.Success
        assertEquals("نام ویرایش‌شده", refreshedState.data.customer.name)
        assertEquals("+989121111111", refreshedState.data.customer.phone)
    }

    /**
     * TEST B (Phase F2 Timing Fix) — the exact bug this phase fixes, for Profile. Constructs the
     * ViewModel while the provider only has the empty placeholder available (matching a ViewModel
     * built before [ai.rojan.designlab.manager.data.ManagerRepositories.initialize] finishes, when
     * `ManagerRepositories.customers` is still `EmptyCustomerRepository`), then changes what the
     * *same* provider returns (matching `initialize()` later replacing the singleton with the real
     * [ai.rojan.designlab.manager.data.BackendCustomerRepository]) and calls [retry]. Before this
     * fix, the ViewModel captured the repository once at construction and `retry()` could never
     * recover; this proves it now does, without leaving and re-entering the screen.
     */
    @Test
    fun `retry after the provider starts returning a different repository uses that new repository, not the one captured at construction`() = runBlocking {
        val emptyRepository = ProfileFakeCustomerRepository() // no customers at all - models EmptyCustomerRepository
        val realRepository = ProfileFakeCustomerRepository(
            customers = mapOf(customer.id to customer),
            history = listOf(historyEntry),
        )
        var currentRepository: CustomerRepository = emptyRepository
        val viewModel = viewModel(customerRepositoryProvider = { currentRepository })

        // Constructed while only the empty placeholder was available - a genuine miss, not a substitute.
        assertEquals(UiState.Empty, viewModel.state)

        // ManagerRepositories.initialize() finishes and replaces the singleton - modeled here as the
        // provider now returning a different repository instance, exactly as `{ ManagerRepositories.customers }`
        // would after that reassignment.
        currentRepository = realRepository

        viewModel.retry()

        val state = viewModel.state as UiState.Success
        assertEquals(customer, state.data.customer)
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
 *
 * [setCustomer] models [ai.rojan.designlab.manager.data.BackendCustomerRepository.update] mutating
 * its cache in place (Phase F1-B regression coverage) - a mutable `var`, not a new repository
 * instance, matching production shape exactly.
 */
private class ProfileFakeCustomerRepository(
    customers: Map<String, ManagerCustomer> = emptyMap(),
    private val history: List<CustomerServiceHistoryEntry> = emptyList(),
    private val notes: List<CustomerNote> = emptyList(),
    private val loadDetailResult: Result<Unit> = Result.success(Unit),
) : CustomerRepository {
    private var customers: Map<String, ManagerCustomer> = customers

    fun setCustomer(customer: ManagerCustomer) {
        customers = customers + (customer.id to customer)
    }

    override fun getAll(): List<ManagerCustomer> = customers.values.toList()
    override fun getById(id: String): ManagerCustomer? = customers[id]
    override fun search(query: String): List<ManagerCustomer> = customers.values.toList()
    override suspend fun create(customer: ManagerCustomer): Result<ManagerCustomer> = error("not used by these tests")
    override suspend fun update(customer: ManagerCustomer): Result<ManagerCustomer?> = error("not used by these tests")
    override fun getServiceHistory(customerId: String): List<CustomerServiceHistoryEntry> = history
    override fun getNoteHistory(customerId: String): List<CustomerNote> = notes
    override suspend fun loadDetail(customerId: String): Result<Unit> = loadDetailResult
}
