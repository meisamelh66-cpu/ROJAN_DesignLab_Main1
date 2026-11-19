package ai.rojan.designlab.manager.presentation.staff

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.manager.domain.membership.SalonMember
import ai.rojan.designlab.manager.domain.membership.SalonMemberRole
import ai.rojan.designlab.manager.domain.membership.StaffCandidate
import ai.rojan.designlab.manager.domain.repository.SalonMembershipRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Staff Access Phase 1 - hermetic (no real backend) coverage of
 * [ManagerStaffAccessViewModel], same fake-repository approach as
 * [ManagerInviteReceptionViewModelTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManagerStaffAccessViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun member(userId: String, role: SalonMemberRole = SalonMemberRole.RECEPTIONIST, active: Boolean = true) =
        SalonMember(id = "membership-$userId", salonId = "salon-1", userId = userId, role = role, active = active, fullName = "Staff $userId", phoneNumber = "+98912000000$userId")

    @Test
    fun `loads the member list on init`() = runTest {
        val repository = FakeSalonMembershipRepository(listResult = { Result.success(listOf(member("1"))) })

        val viewModel = ManagerStaffAccessViewModel(salonId = "salon-1", repository = repository)

        val loaded = viewModel.state.value as StaffAccessUiState.Loaded
        assertEquals(1, loaded.members.size)
    }

    @Test
    fun `missing active salon surfaces an error without calling the repository`() {
        val repository = FakeSalonMembershipRepository(listResult = { error("must not be called without a salon") })

        val viewModel = ManagerStaffAccessViewModel(salonId = null, repository = repository)

        assertTrue(viewModel.state.value is StaffAccessUiState.Error)
    }

    @Test
    fun `a listMembers failure surfaces an Error state`() = runTest {
        val repository = FakeSalonMembershipRepository(
            listResult = { Result.failure(BackendApiException(403, null)) },
        )

        val viewModel = ManagerStaffAccessViewModel(salonId = "salon-1", repository = repository)

        assertTrue(viewModel.state.value is StaffAccessUiState.Error)
    }

    @Test
    fun `startAddStaff opens the EnteringPhone sub-flow, cancelAddStaff hides it again`() {
        val viewModel = ManagerStaffAccessViewModel(salonId = "salon-1", repository = FakeSalonMembershipRepository())

        viewModel.startAddStaff()
        assertTrue(viewModel.addState.value is AddStaffUiState.EnteringPhone)

        viewModel.cancelAddStaff()
        assertTrue(viewModel.addState.value is AddStaffUiState.Hidden)
    }

    @Test
    fun `lookupPhone rejects a malformed phone number without calling the repository`() {
        val repository = FakeSalonMembershipRepository(lookupResult = { _, _ -> error("must not be called for a malformed phone") })
        val viewModel = ManagerStaffAccessViewModel(salonId = "salon-1", repository = repository)
        viewModel.startAddStaff()

        viewModel.onPhoneInputChange("abc")
        viewModel.lookupPhone()

        val state = viewModel.addState.value as AddStaffUiState.EnteringPhone
        assertEquals("abc", state.phone)
        assertTrue(state.error != null)
    }

    @Test
    fun `lookupPhone normalizes a local 09-prefixed number before calling the repository`() = runTest {
        var capturedPhone: String? = null
        val repository = FakeSalonMembershipRepository(
            lookupResult = { _, phone -> capturedPhone = phone; Result.success(StaffCandidate("user-1", "Candidate", "+989120000001")) },
        )
        val viewModel = ManagerStaffAccessViewModel(salonId = "salon-1", repository = repository)
        viewModel.startAddStaff()

        viewModel.onPhoneInputChange("09120000001")
        viewModel.lookupPhone()

        assertEquals("+989120000001", capturedPhone)
        assertTrue(viewModel.addState.value is AddStaffUiState.Confirming)
    }

    @Test
    fun `lookupPhone not-found surfaces an Error in the add-flow`() = runTest {
        val repository = FakeSalonMembershipRepository(
            lookupResult = { _, _ -> Result.failure(BackendApiException(404, null)) },
        )
        val viewModel = ManagerStaffAccessViewModel(salonId = "salon-1", repository = repository)
        viewModel.startAddStaff()

        viewModel.onPhoneInputChange("09120000002")
        viewModel.lookupPhone()

        assertTrue(viewModel.addState.value is AddStaffUiState.Error)
    }

    @Test
    fun `confirmAssign assigns the selected role, then hides the flow and reloads the list`() = runTest {
        var capturedRole: SalonMemberRole? = null
        val repository = FakeSalonMembershipRepository(
            lookupResult = { _, phone -> Result.success(StaffCandidate("user-1", "Candidate", phone)) },
            assignResult = { _, _, role -> capturedRole = role; Result.success(member("1", role = role)) },
            listResult = { Result.success(listOf(member("1", role = SalonMemberRole.MANAGER))) },
        )
        val viewModel = ManagerStaffAccessViewModel(salonId = "salon-1", repository = repository)
        viewModel.startAddStaff()
        viewModel.onPhoneInputChange("09120000001")
        viewModel.lookupPhone()
        viewModel.onRoleSelected(SalonMemberRole.MANAGER)

        viewModel.confirmAssign()

        assertEquals(SalonMemberRole.MANAGER, capturedRole)
        assertTrue(viewModel.addState.value is AddStaffUiState.Hidden)
        val loaded = viewModel.state.value as StaffAccessUiState.Loaded
        assertEquals(SalonMemberRole.MANAGER, loaded.members.first().role)
    }

    @Test
    fun `setMemberActive(false) disables a member then reloads the list`() = runTest {
        var capturedActive: Boolean? = null
        val repository = FakeSalonMembershipRepository(
            listResult = { Result.success(listOf(member("1", active = capturedActive ?: true))) },
            setActiveResult = { _, _, active -> capturedActive = active; Result.success(member("1", active = active)) },
        )
        val viewModel = ManagerStaffAccessViewModel(salonId = "salon-1", repository = repository)

        viewModel.setMemberActive("user-1", active = false)

        assertEquals(false, capturedActive)
        val loaded = viewModel.state.value as StaffAccessUiState.Loaded
        assertEquals(false, loaded.members.first().active)
    }

    @Test
    fun `changeRole reassigns the role then reloads the list`() = runTest {
        val repository = FakeSalonMembershipRepository(
            listResult = { Result.success(listOf(member("1", role = SalonMemberRole.MANAGER))) },
            assignResult = { _, _, role -> Result.success(member("1", role = role)) },
        )
        val viewModel = ManagerStaffAccessViewModel(salonId = "salon-1", repository = repository)

        viewModel.changeRole("user-1", SalonMemberRole.MANAGER)

        val loaded = viewModel.state.value as StaffAccessUiState.Loaded
        assertEquals(SalonMemberRole.MANAGER, loaded.members.first().role)
    }

    @Test
    fun `normalizeIranianPhone accepts E164 as-is, local 09-prefixed, and 98-prefixed - rejects garbage`() {
        assertEquals("+989120000001", normalizeIranianPhone("+989120000001"))
        assertEquals("+989120000001", normalizeIranianPhone("09120000001"))
        assertEquals("+989120000001", normalizeIranianPhone("989120000001"))
        assertNull(normalizeIranianPhone("abc"))
        assertNull(normalizeIranianPhone(""))
    }
}

private class FakeSalonMembershipRepository(
    private val listResult: suspend () -> Result<List<SalonMember>> = { Result.success(emptyList()) },
    private val lookupResult: suspend (salonId: String, phone: String) -> Result<StaffCandidate> = { _, _ -> error("not used by this test") },
    private val assignResult: suspend (salonId: String, userId: String, role: SalonMemberRole) -> Result<SalonMember> = { _, _, _ -> error("not used by this test") },
    private val setActiveResult: suspend (salonId: String, userId: String, active: Boolean) -> Result<SalonMember> = { _, _, _ -> error("not used by this test") },
) : SalonMembershipRepository {
    override suspend fun listMembers(salonId: String): Result<List<SalonMember>> = listResult()
    override suspend fun lookupByPhone(salonId: String, phone: String): Result<StaffCandidate> = lookupResult(salonId, phone)
    override suspend fun assignRole(salonId: String, userId: String, role: SalonMemberRole): Result<SalonMember> =
        assignResult(salonId, userId, role)
    override suspend fun setActive(salonId: String, userId: String, active: Boolean): Result<SalonMember> =
        setActiveResult(salonId, userId, active)
    override suspend fun removeMember(salonId: String, userId: String): Result<Unit> = error("not used by this test")
}
