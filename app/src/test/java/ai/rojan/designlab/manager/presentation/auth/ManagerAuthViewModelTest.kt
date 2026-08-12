package ai.rojan.designlab.manager.presentation.auth

import ai.rojan.designlab.domain.repository.ActiveSalonContextRepository
import ai.rojan.designlab.domain.repository.AuthSessionRepository
import ai.rojan.designlab.domain.repository.AuthenticatedUser
import ai.rojan.designlab.domain.repository.BackendAuthRepository
import ai.rojan.designlab.domain.repository.CurrentUserIdentityContext
import ai.rojan.designlab.domain.repository.CurrentUserIdentityContextRepository
import ai.rojan.designlab.domain.repository.OtpIssued
import ai.rojan.designlab.domain.repository.OwnedSalonAccess
import ai.rojan.designlab.domain.repository.SalonMembershipAccess
import ai.rojan.designlab.domain.repository.TokenRepository
import ai.rojan.designlab.manager.domain.auth.ActiveSalonUiState
import ai.rojan.designlab.manager.domain.auth.ManagerAuthState
import ai.rojan.designlab.manager.domain.auth.ManagerOtpStep
import ai.rojan.designlab.presentation.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
 * OTP Authentication Entry Flow Integration — hermetic (no real backend,
 * no Android framework) coverage of [ManagerAuthViewModel]'s gate/OTP/
 * logout logic, using in-memory fakes for [AuthSessionRepository]/
 * [BackendAuthRepository]/[TokenRepository] — same fake-repository
 * approach `ROJAN_Backend`'s own use-case tests already establish, applied
 * here for the first time to a mobile ViewModel in this codebase.
 *
 * Covers the five scenarios required by this integration's own report:
 * fresh install, existing valid JWT, expired JWT, successful OTP
 * verification, and logout — plus two additional cases this
 * implementation specifically guards against (a valid session or a
 * successful OTP verification belonging to a non-MANAGER account), since
 * the OTP API itself has no way to assert role at signup (see
 * [ManagerAuthViewModel.onVerified]'s own doc comment).
 *
 * Also covers Active Salon Context & Selection Flow's resolution rules
 * (auto-select on exactly one salon, required choice on more than one,
 * persisted-selection-still-valid skips the prompt, zero-salon error,
 * explicit [ManagerAuthViewModel.selectSalon], and logout clearing it).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManagerAuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val managerUser = AuthenticatedUser(id = "user-1", email = "manager@example.com", fullName = "Test Manager", role = "MANAGER")
    private val customerUser = AuthenticatedUser(id = "user-2", email = "customer@example.com", fullName = "Test Customer", role = "CUSTOMER")

    // --- Scenario 1: Fresh install -----------------------------------

    @Test
    fun `fresh install with no persisted session lands on the OTP entry step, unauthenticated`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository(initialPersonId = null)
        val backendAuthRepository = FakeBackendAuthRepository()
        val tokenRepository = FakeTokenRepository()

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository, FakeCurrentUserIdentityContextRepository(), FakeActiveSalonContextRepository())

        assertEquals(ManagerAuthState.Unauthenticated, viewModel.authState.value)
        assertEquals(ManagerOtpStep.EnteringPhone, viewModel.otpStep.value)
    }

    // --- Scenario 2: Existing valid JWT --------------------------------

    @Test
    fun `existing valid session for a manager account restores straight to Authenticated`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository(initialPersonId = managerUser.id)
        val backendAuthRepository = FakeBackendAuthRepository(currentUserResult = Result.success(managerUser))
        val tokenRepository = FakeTokenRepository()

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository, FakeCurrentUserIdentityContextRepository(), FakeActiveSalonContextRepository())

        val state = viewModel.authState.value
        assertTrue(state is ManagerAuthState.Authenticated)
        assertEquals(managerUser.id, (state as ManagerAuthState.Authenticated).userId)
    }

    @Test
    fun `existing valid session for a non-manager account is treated as unauthenticated and cleared`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository(initialPersonId = customerUser.id)
        val backendAuthRepository = FakeBackendAuthRepository(currentUserResult = Result.success(customerUser))
        val tokenRepository = FakeTokenRepository()

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository, FakeCurrentUserIdentityContextRepository(), FakeActiveSalonContextRepository())

        assertEquals(ManagerAuthState.Unauthenticated, viewModel.authState.value)
        assertEquals(1, authSessionRepository.clearPersonIdCallCount)
        assertEquals(1, tokenRepository.clearTokensCallCount)
    }

    // --- Scenario 3: Expired JWT ---------------------------------------

    @Test
    fun `expired or unrecoverable session falls back to unauthenticated and clears the stale session`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository(initialPersonId = "some-persisted-id")
        val backendAuthRepository = FakeBackendAuthRepository(
            currentUserResult = Result.failure(IllegalStateException("401 - refresh token also invalid")),
        )
        val tokenRepository = FakeTokenRepository()
        tokenRepository.saveTokens("stale-access", "stale-refresh")

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository, FakeCurrentUserIdentityContextRepository(), FakeActiveSalonContextRepository())

        assertEquals(ManagerAuthState.Unauthenticated, viewModel.authState.value)
        assertEquals(ManagerOtpStep.EnteringPhone, viewModel.otpStep.value)
        assertNull(tokenRepository.accessToken())
        assertNull(tokenRepository.refreshToken())
        assertEquals(1, authSessionRepository.clearPersonIdCallCount)
    }

    // --- Scenario 4: Successful OTP verification ------------------------

    @Test
    fun `requesting then verifying an OTP for a manager account saves the JWT and authenticates`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository(initialPersonId = null)
        val backendAuthRepository = FakeBackendAuthRepository(
            requestOtpResult = Result.success(OtpIssued("+989123456789", expiresInSeconds = 120, canResendAfterSeconds = 60)),
            verifyOtpResult = Result.success(managerUser),
        )
        val tokenRepository = FakeTokenRepository()

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository, FakeCurrentUserIdentityContextRepository(), FakeActiveSalonContextRepository())
        assertEquals(ManagerAuthState.Unauthenticated, viewModel.authState.value)

        viewModel.requestOtp("+989123456789")
        val stepAfterRequest = viewModel.otpStep.value
        assertTrue(stepAfterRequest is ManagerOtpStep.AwaitingCode)
        assertEquals("+989123456789", (stepAfterRequest as ManagerOtpStep.AwaitingCode).phoneNumber)

        backendAuthRepository.verifyOtpCall = { phoneNumber, code ->
            assertEquals("+989123456789", phoneNumber)
            assertEquals("482913", code)
        }
        viewModel.verifyOtp("482913")

        val finalState = viewModel.authState.value
        assertTrue(finalState is ManagerAuthState.Authenticated)
        assertEquals(managerUser.id, (finalState as ManagerAuthState.Authenticated).userId)
        assertEquals(managerUser.id, authSessionRepository.savedPersonId)
    }

    @Test
    fun `verifying an OTP for a non-manager account is rejected without authenticating`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository(initialPersonId = null)
        val backendAuthRepository = FakeBackendAuthRepository(
            requestOtpResult = Result.success(OtpIssued("+989123456789", expiresInSeconds = 120, canResendAfterSeconds = 60)),
            verifyOtpResult = Result.success(customerUser),
        )
        val tokenRepository = FakeTokenRepository()

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository, FakeCurrentUserIdentityContextRepository(), FakeActiveSalonContextRepository())
        viewModel.requestOtp("+989123456789")
        viewModel.verifyOtp("482913")

        assertEquals(ManagerAuthState.Unauthenticated, viewModel.authState.value)
        assertNull(authSessionRepository.savedPersonId)
        assertTrue(viewModel.errorMessage.value != null)
    }

    @Test
    fun `a wrong or expired OTP code stays on the code entry step with an error, unauthenticated`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository(initialPersonId = null)
        val backendAuthRepository = FakeBackendAuthRepository(
            requestOtpResult = Result.success(OtpIssued("+989123456789", expiresInSeconds = 120, canResendAfterSeconds = 60)),
            verifyOtpResult = Result.failure(IllegalStateException("401 - invalid or expired code")),
        )
        val tokenRepository = FakeTokenRepository()

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository, FakeCurrentUserIdentityContextRepository(), FakeActiveSalonContextRepository())
        viewModel.requestOtp("+989123456789")
        viewModel.verifyOtp("000000")

        assertEquals(ManagerAuthState.Unauthenticated, viewModel.authState.value)
        assertTrue(viewModel.otpStep.value is ManagerOtpStep.AwaitingCode)
        assertTrue(viewModel.errorMessage.value != null)
    }

    // --- Scenario 5: Logout ---------------------------------------------

    @Test
    fun `logout clears the session and returns to the OTP entry step`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository(initialPersonId = managerUser.id)
        val backendAuthRepository = FakeBackendAuthRepository(currentUserResult = Result.success(managerUser))
        val tokenRepository = FakeTokenRepository()
        tokenRepository.saveTokens("access", "refresh")

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository, FakeCurrentUserIdentityContextRepository(), FakeActiveSalonContextRepository())
        assertTrue(viewModel.authState.value is ManagerAuthState.Authenticated)

        viewModel.logout()

        assertEquals(ManagerAuthState.Unauthenticated, viewModel.authState.value)
        assertEquals(ManagerOtpStep.EnteringPhone, viewModel.otpStep.value)
        assertNull(tokenRepository.accessToken())
        assertNull(tokenRepository.refreshToken())
        assertEquals(1, authSessionRepository.clearPersonIdCallCount)
    }

    // --- Identity & Session Architecture, Android Integration -----------

    private fun emptyIdentityContext() = CurrentUserIdentityContext(
        userId = managerUser.id,
        phoneNumber = managerUser.phoneNumber,
        email = managerUser.email,
        fullName = managerUser.fullName,
        globalRole = managerUser.role,
        ownedSalons = emptyList(),
        memberships = emptyList(),
        specialistLinks = emptyList(),
    )

    @Test
    fun `salon-access is fetched after a successful manager OTP verification`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository(initialPersonId = null)
        val backendAuthRepository = FakeBackendAuthRepository(
            requestOtpResult = Result.success(OtpIssued("+989123456789", expiresInSeconds = 120, canResendAfterSeconds = 60)),
            verifyOtpResult = Result.success(managerUser),
        )
        val tokenRepository = FakeTokenRepository()
        val identityContextRepository = FakeCurrentUserIdentityContextRepository(Result.success(emptyIdentityContext()))

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository, identityContextRepository, FakeActiveSalonContextRepository())
        viewModel.requestOtp("+989123456789")
        viewModel.verifyOtp("482913")

        assertEquals(1, identityContextRepository.callCount)
        assertTrue(viewModel.identityContext.value is UiState.Success<CurrentUserIdentityContext>)
    }

    @Test
    fun `a salon-access failure does not roll back an already-authenticated manager session`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository(initialPersonId = null)
        val backendAuthRepository = FakeBackendAuthRepository(
            requestOtpResult = Result.success(OtpIssued("+989123456789", expiresInSeconds = 120, canResendAfterSeconds = 60)),
            verifyOtpResult = Result.success(managerUser),
        )
        val tokenRepository = FakeTokenRepository()
        val identityContextRepository = FakeCurrentUserIdentityContextRepository(Result.failure(IllegalStateException("network down")))

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository, identityContextRepository, FakeActiveSalonContextRepository())
        viewModel.requestOtp("+989123456789")
        viewModel.verifyOtp("482913")

        assertTrue(viewModel.identityContext.value is UiState.Error)
        assertTrue(viewModel.authState.value is ManagerAuthState.Authenticated)
    }

    @Test
    fun `session restore re-fetches a fresh salon-access context for a manager`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository(initialPersonId = managerUser.id)
        val backendAuthRepository = FakeBackendAuthRepository(currentUserResult = Result.success(managerUser))
        val tokenRepository = FakeTokenRepository()
        val identityContextRepository = FakeCurrentUserIdentityContextRepository(Result.success(emptyIdentityContext()))

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository, identityContextRepository, FakeActiveSalonContextRepository())

        assertEquals(1, identityContextRepository.callCount)
        assertTrue(viewModel.identityContext.value is UiState.Success<CurrentUserIdentityContext>)
    }

    @Test
    fun `logout resets the manager identity context back to loading`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository(initialPersonId = managerUser.id)
        val backendAuthRepository = FakeBackendAuthRepository(currentUserResult = Result.success(managerUser))
        val tokenRepository = FakeTokenRepository()
        val identityContextRepository = FakeCurrentUserIdentityContextRepository(Result.success(emptyIdentityContext()))
        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository, identityContextRepository, FakeActiveSalonContextRepository())
        assertTrue(viewModel.identityContext.value is UiState.Success<CurrentUserIdentityContext>)

        viewModel.logout()

        assertEquals(UiState.Loading, viewModel.identityContext.value)
    }

    // --- Active Salon Context & Selection Flow ---------------------------

    @Test
    fun `exactly one available salon is auto-selected and persisted`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository(initialPersonId = null)
        val backendAuthRepository = FakeBackendAuthRepository(
            requestOtpResult = Result.success(OtpIssued("+989123456789", expiresInSeconds = 120, canResendAfterSeconds = 60)),
            verifyOtpResult = Result.success(managerUser),
        )
        val tokenRepository = FakeTokenRepository()
        val owned = OwnedSalonAccess(salonId = "s1", salonName = "Salon One", active = true, permissions = setOf("MANAGE_SALON"))
        val identityContextRepository = FakeCurrentUserIdentityContextRepository(
            Result.success(emptyIdentityContext().copy(ownedSalons = listOf(owned))),
        )
        val activeSalonContextRepository = FakeActiveSalonContextRepository()

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository, identityContextRepository, activeSalonContextRepository)
        viewModel.requestOtp("+989123456789")
        viewModel.verifyOtp("482913")

        val state = viewModel.activeSalonState.value
        assertTrue(state is ActiveSalonUiState.Active)
        assertEquals("s1", (state as ActiveSalonUiState.Active).context.salonId)
        assertEquals(1, activeSalonContextRepository.saveCallCount)
    }

    @Test
    fun `more than one available salon with no valid persisted selection requires a choice`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository(initialPersonId = managerUser.id)
        val backendAuthRepository = FakeBackendAuthRepository(currentUserResult = Result.success(managerUser))
        val tokenRepository = FakeTokenRepository()
        val owned = OwnedSalonAccess(salonId = "s1", salonName = "Salon One", active = true, permissions = emptySet())
        val membership = SalonMembershipAccess(membershipId = "m1", salonId = "s2", salonName = "Salon Two", active = true, role = "STAFF", permissions = emptySet())
        val identityContextRepository = FakeCurrentUserIdentityContextRepository(
            Result.success(emptyIdentityContext().copy(ownedSalons = listOf(owned), memberships = listOf(membership))),
        )
        val activeSalonContextRepository = FakeActiveSalonContextRepository()

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository, identityContextRepository, activeSalonContextRepository)

        val state = viewModel.activeSalonState.value
        assertTrue(state is ActiveSalonUiState.SelectionRequired)
        assertEquals(2, (state as ActiveSalonUiState.SelectionRequired).options.size)
        assertEquals(0, activeSalonContextRepository.saveCallCount)
    }

    @Test
    fun `a previously persisted salon id still among the available options resolves directly without prompting`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository(initialPersonId = managerUser.id)
        val backendAuthRepository = FakeBackendAuthRepository(currentUserResult = Result.success(managerUser))
        val tokenRepository = FakeTokenRepository()
        val owned = OwnedSalonAccess(salonId = "s1", salonName = "Salon One", active = true, permissions = emptySet())
        val membership = SalonMembershipAccess(membershipId = "m1", salonId = "s2", salonName = "Salon Two", active = true, role = "STAFF", permissions = emptySet())
        val identityContextRepository = FakeCurrentUserIdentityContextRepository(
            Result.success(emptyIdentityContext().copy(ownedSalons = listOf(owned), memberships = listOf(membership))),
        )
        val activeSalonContextRepository = FakeActiveSalonContextRepository(initialSalonId = "s2")

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository, identityContextRepository, activeSalonContextRepository)

        val state = viewModel.activeSalonState.value
        assertTrue(state is ActiveSalonUiState.Active)
        assertEquals("s2", (state as ActiveSalonUiState.Active).context.salonId)
        assertEquals(0, activeSalonContextRepository.saveCallCount)
    }

    @Test
    fun `zero available salons resolves to an error state, never a crash`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository(initialPersonId = managerUser.id)
        val backendAuthRepository = FakeBackendAuthRepository(currentUserResult = Result.success(managerUser))
        val tokenRepository = FakeTokenRepository()
        val identityContextRepository = FakeCurrentUserIdentityContextRepository(Result.success(emptyIdentityContext()))
        val activeSalonContextRepository = FakeActiveSalonContextRepository()

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository, identityContextRepository, activeSalonContextRepository)

        assertTrue(viewModel.activeSalonState.value is ActiveSalonUiState.Error)
    }

    @Test
    fun `selecting a salon from a required choice persists it and resolves to Active`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository(initialPersonId = managerUser.id)
        val backendAuthRepository = FakeBackendAuthRepository(currentUserResult = Result.success(managerUser))
        val tokenRepository = FakeTokenRepository()
        val owned = OwnedSalonAccess(salonId = "s1", salonName = "Salon One", active = true, permissions = emptySet())
        val membership = SalonMembershipAccess(membershipId = "m1", salonId = "s2", salonName = "Salon Two", active = true, role = "STAFF", permissions = emptySet())
        val identityContextRepository = FakeCurrentUserIdentityContextRepository(
            Result.success(emptyIdentityContext().copy(ownedSalons = listOf(owned), memberships = listOf(membership))),
        )
        val activeSalonContextRepository = FakeActiveSalonContextRepository()

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository, identityContextRepository, activeSalonContextRepository)
        val required = viewModel.activeSalonState.value as ActiveSalonUiState.SelectionRequired
        val chosen = required.options.first { it.salonId == "s2" }

        viewModel.selectSalon(chosen)

        val state = viewModel.activeSalonState.value
        assertTrue(state is ActiveSalonUiState.Active)
        assertEquals("s2", (state as ActiveSalonUiState.Active).context.salonId)
        assertEquals(1, activeSalonContextRepository.saveCallCount)
    }

    @Test
    fun `logout clears the persisted active salon and resets its state to loading`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository(initialPersonId = managerUser.id)
        val backendAuthRepository = FakeBackendAuthRepository(currentUserResult = Result.success(managerUser))
        val tokenRepository = FakeTokenRepository()
        val owned = OwnedSalonAccess(salonId = "s1", salonName = "Salon One", active = true, permissions = emptySet())
        val identityContextRepository = FakeCurrentUserIdentityContextRepository(
            Result.success(emptyIdentityContext().copy(ownedSalons = listOf(owned))),
        )
        val activeSalonContextRepository = FakeActiveSalonContextRepository()
        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository, identityContextRepository, activeSalonContextRepository)
        assertTrue(viewModel.activeSalonState.value is ActiveSalonUiState.Active)

        viewModel.logout()

        assertEquals(ActiveSalonUiState.Loading, viewModel.activeSalonState.value)
        assertEquals(1, activeSalonContextRepository.clearCallCount)
    }

    // --- Fakes -----------------------------------------------------------

    private class FakeCurrentUserIdentityContextRepository(
        private val result: Result<CurrentUserIdentityContext> = Result.failure(IllegalStateException("not stubbed")),
    ) : CurrentUserIdentityContextRepository {
        var callCount = 0
            private set

        override suspend fun getCurrentUserIdentityContext(): Result<CurrentUserIdentityContext> {
            callCount++
            return result
        }
    }

    private class FakeActiveSalonContextRepository(initialSalonId: String? = null) : ActiveSalonContextRepository {
        private val salonId = MutableStateFlow(initialSalonId)

        var saveCallCount = 0
            private set
        var clearCallCount = 0
            private set

        override suspend fun saveActiveSalonId(salonId: String) {
            this.salonId.value = salonId
            saveCallCount++
        }

        override suspend fun clearActiveSalonId() {
            salonId.value = null
            clearCallCount++
        }

        override fun observeActiveSalonId(): Flow<String?> = salonId
    }

    private class FakeAuthSessionRepository(initialPersonId: String?) : AuthSessionRepository {
        private val personId = MutableStateFlow(initialPersonId)

        var savedPersonId: String? = null
            private set
        var clearPersonIdCallCount = 0
            private set

        override suspend fun savePersonId(personId: String) {
            this.personId.value = personId
            savedPersonId = personId
        }

        override suspend fun clearPersonId() {
            personId.value = null
            savedPersonId = null
            clearPersonIdCallCount++
        }

        override fun observePersonId(): Flow<String?> = personId
    }

    private class FakeTokenRepository : TokenRepository {
        private var access: String? = null
        private var refresh: String? = null

        var clearTokensCallCount = 0
            private set

        override fun saveTokens(accessToken: String, refreshToken: String) {
            access = accessToken
            refresh = refreshToken
        }

        override fun clearTokens() {
            access = null
            refresh = null
            clearTokensCallCount++
        }

        override fun accessToken(): String? = access
        override fun refreshToken(): String? = refresh
    }

    private class FakeBackendAuthRepository(
        private val currentUserResult: Result<AuthenticatedUser> = Result.failure(IllegalStateException("not stubbed")),
        private val requestOtpResult: Result<OtpIssued> = Result.failure(IllegalStateException("not stubbed")),
        private val verifyOtpResult: Result<AuthenticatedUser> = Result.failure(IllegalStateException("not stubbed")),
    ) : BackendAuthRepository {

        var verifyOtpCall: ((phoneNumber: String, code: String) -> Unit)? = null

        override suspend fun register(email: String, password: String, fullName: String): Result<AuthenticatedUser> =
            error("not used by ManagerAuthViewModel")

        override suspend fun login(email: String, password: String): Result<AuthenticatedUser> =
            error("not used by ManagerAuthViewModel — Manager App is OTP-only")

        override suspend fun currentUser(): Result<AuthenticatedUser> = currentUserResult

        override suspend fun requestOtp(phoneNumber: String): Result<OtpIssued> = requestOtpResult

        override suspend fun verifyOtp(phoneNumber: String, code: String, fullName: String?): Result<AuthenticatedUser> {
            verifyOtpCall?.invoke(phoneNumber, code)
            return verifyOtpResult
        }
    }
}
