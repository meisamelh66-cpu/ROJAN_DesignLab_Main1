package ai.rojan.designlab.reception.presentation.auth

import ai.rojan.designlab.domain.repository.ActiveSalonContextRepository
import ai.rojan.designlab.domain.repository.AuthSessionRepository
import ai.rojan.designlab.domain.repository.AuthenticatedUser
import ai.rojan.designlab.domain.repository.BackendAuthRepository
import ai.rojan.designlab.domain.repository.CurrentUserIdentityContext
import ai.rojan.designlab.domain.repository.CurrentUserIdentityContextRepository
import ai.rojan.designlab.domain.repository.OtpIssued
import ai.rojan.designlab.domain.repository.OwnedSalonAccess
import ai.rojan.designlab.domain.repository.TokenRepository
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.reception.domain.auth.ActiveSalonUiState
import ai.rojan.designlab.reception.domain.auth.ReceptionAuthState
import ai.rojan.designlab.reception.domain.auth.ReceptionOtpStep
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Hermetic (no real backend, no Android framework) coverage of
 * [ReceptionAuthViewModel]'s resilient blocked-state behavior — added
 * alongside that Phase 1 change, same in-memory-fake approach
 * [ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModelTest]
 * already establishes for the structurally-identical Manager class.
 *
 * Scoped deliberately narrow: this covers the bug this phase actually
 * fixed (a `/salon-access` failure previously left [ReceptionAuthViewModel.activeSalonState]
 * stuck at [ActiveSalonUiState.Loading] forever instead of resolving to
 * [ActiveSalonUiState.Error]) plus [ReceptionAuthViewModel.retryIdentityResolution],
 * not a full re-derivation of every scenario
 * `ManagerAuthViewModelTest` already covers generically for the identical
 * underlying gate/OTP/logout shape.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReceptionAuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val receptionUser = AuthenticatedUser(id = "user-1", email = "reception@example.com", fullName = "Test Reception", role = "MANAGER")

    private fun emptyIdentityContext() = CurrentUserIdentityContext(
        userId = receptionUser.id,
        phoneNumber = receptionUser.phoneNumber,
        email = receptionUser.email,
        fullName = receptionUser.fullName,
        globalRole = receptionUser.role,
        ownedSalons = emptyList(),
        memberships = emptyList(),
        specialistLinks = emptyList(),
    )

    // --- The actual bug fix ----------------------------------------------

    @Test
    fun `a salon-access failure resolves activeSalonState to Error, not stuck at Loading`() = runTest {
        val identityContextRepository = FakeCurrentUserIdentityContextRepository(
            Result.failure(IllegalStateException("404 - /users/me/salon-access not found")),
        )

        val viewModel = ReceptionAuthViewModel(
            FakeAuthSessionRepository(initialPersonId = receptionUser.id),
            FakeBackendAuthRepository(currentUserResult = Result.success(receptionUser)),
            FakeTokenRepository(),
            identityContextRepository,
            FakeActiveSalonContextRepository(),
        )

        assertTrue(viewModel.identityContext.value is UiState.Error)
        assertTrue(
            "activeSalonState must not stay Loading forever on a fetch failure",
            viewModel.activeSalonState.value is ActiveSalonUiState.Error,
        )
    }

    @Test
    fun `the Error message surfaced to activeSalonState matches the identity-context failure`() = runTest {
        val identityContextRepository = FakeCurrentUserIdentityContextRepository(
            Result.failure(IllegalStateException("network unreachable")),
        )

        val viewModel = ReceptionAuthViewModel(
            FakeAuthSessionRepository(initialPersonId = receptionUser.id),
            FakeBackendAuthRepository(currentUserResult = Result.success(receptionUser)),
            FakeTokenRepository(),
            identityContextRepository,
            FakeActiveSalonContextRepository(),
        )

        val identityMessage = (viewModel.identityContext.value as UiState.Error).message
        val salonMessage = (viewModel.activeSalonState.value as ActiveSalonUiState.Error).message
        assertEquals(identityMessage, salonMessage)
    }

    @Test
    fun `retryIdentityResolution re-attempts and can resolve to Active after a prior failure`() = runTest {
        val owned = OwnedSalonAccess(salonId = "s1", salonName = "Salon One", active = true, permissions = setOf("MANAGE_OWN_BOOKINGS"))
        val identityContextRepository = FakeCurrentUserIdentityContextRepository(Result.failure(IllegalStateException("404")))

        val viewModel = ReceptionAuthViewModel(
            FakeAuthSessionRepository(initialPersonId = receptionUser.id),
            FakeBackendAuthRepository(currentUserResult = Result.success(receptionUser)),
            FakeTokenRepository(),
            identityContextRepository,
            FakeActiveSalonContextRepository(),
        )
        assertTrue(viewModel.activeSalonState.value is ActiveSalonUiState.Error)

        identityContextRepository.result = Result.success(emptyIdentityContext().copy(ownedSalons = listOf(owned)))
        viewModel.retryIdentityResolution()

        val state = viewModel.activeSalonState.value
        assertTrue(state is ActiveSalonUiState.Active)
        assertEquals("s1", (state as ActiveSalonUiState.Active).context.salonId)
    }

    @Test
    fun `retryIdentityResolution that fails again stays at Error, still not stuck at Loading`() = runTest {
        val identityContextRepository = FakeCurrentUserIdentityContextRepository(Result.failure(IllegalStateException("still down")))

        val viewModel = ReceptionAuthViewModel(
            FakeAuthSessionRepository(initialPersonId = receptionUser.id),
            FakeBackendAuthRepository(currentUserResult = Result.success(receptionUser)),
            FakeTokenRepository(),
            identityContextRepository,
            FakeActiveSalonContextRepository(),
        )

        viewModel.retryIdentityResolution()

        assertTrue(viewModel.activeSalonState.value is ActiveSalonUiState.Error)
        assertEquals(2, identityContextRepository.callCount)
    }

    @Test
    fun `zero available salons still resolves to Error, unaffected by the failure-path fix`() = runTest {
        val identityContextRepository = FakeCurrentUserIdentityContextRepository(Result.success(emptyIdentityContext()))

        val viewModel = ReceptionAuthViewModel(
            FakeAuthSessionRepository(initialPersonId = receptionUser.id),
            FakeBackendAuthRepository(currentUserResult = Result.success(receptionUser)),
            FakeTokenRepository(),
            identityContextRepository,
            FakeActiveSalonContextRepository(),
        )

        assertTrue(viewModel.activeSalonState.value is ActiveSalonUiState.Error)
    }

    // --- Baseline gate coverage (same shape as ManagerAuthViewModelTest, scoped to Reception's own class) ---

    @Test
    fun `fresh install with no persisted session lands on the OTP entry step, unauthenticated`() = runTest {
        val viewModel = ReceptionAuthViewModel(
            FakeAuthSessionRepository(initialPersonId = null),
            FakeBackendAuthRepository(),
            FakeTokenRepository(),
            FakeCurrentUserIdentityContextRepository(),
            FakeActiveSalonContextRepository(),
        )

        assertEquals(ReceptionAuthState.Unauthenticated, viewModel.authState.value)
        assertEquals(ReceptionOtpStep.EnteringPhone, viewModel.otpStep.value)
    }

    @Test
    fun `requesting then verifying an OTP saves the JWT and authenticates`() = runTest {
        val backendAuthRepository = FakeBackendAuthRepository(
            requestOtpResult = Result.success(OtpIssued("+989123456789", expiresInSeconds = 120, canResendAfterSeconds = 60)),
            verifyOtpResult = Result.success(receptionUser),
        )
        val authSessionRepository = FakeAuthSessionRepository(initialPersonId = null)

        val viewModel = ReceptionAuthViewModel(
            authSessionRepository, backendAuthRepository, FakeTokenRepository(),
            FakeCurrentUserIdentityContextRepository(), FakeActiveSalonContextRepository(),
        )

        viewModel.requestOtp("+989123456789")
        viewModel.verifyOtp("482913")

        val finalState = viewModel.authState.value
        assertTrue(finalState is ReceptionAuthState.Authenticated)
        assertEquals(receptionUser.id, authSessionRepository.savedPersonId)
    }

    @Test
    fun `logout resets activeSalonState back to Loading, clearing a prior Error`() = runTest {
        val identityContextRepository = FakeCurrentUserIdentityContextRepository(Result.failure(IllegalStateException("404")))
        val viewModel = ReceptionAuthViewModel(
            FakeAuthSessionRepository(initialPersonId = receptionUser.id),
            FakeBackendAuthRepository(currentUserResult = Result.success(receptionUser)),
            FakeTokenRepository(),
            identityContextRepository,
            FakeActiveSalonContextRepository(),
        )
        assertTrue(viewModel.activeSalonState.value is ActiveSalonUiState.Error)

        viewModel.logout()

        assertEquals(ActiveSalonUiState.Loading, viewModel.activeSalonState.value)
        assertEquals(ReceptionAuthState.Unauthenticated, viewModel.authState.value)
    }

    // --- Fakes -------------------------------------------------------------

    private class FakeCurrentUserIdentityContextRepository(
        var result: Result<CurrentUserIdentityContext> = Result.failure(IllegalStateException("not stubbed")),
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

        override suspend fun saveActiveSalonId(salonId: String) {
            this.salonId.value = salonId
        }

        override suspend fun clearActiveSalonId() {
            salonId.value = null
        }

        override fun observeActiveSalonId(): Flow<String?> = salonId
    }

    private class FakeAuthSessionRepository(initialPersonId: String?) : AuthSessionRepository {
        private val personId = MutableStateFlow(initialPersonId)

        var savedPersonId: String? = null
            private set

        override suspend fun savePersonId(personId: String) {
            this.personId.value = personId
            savedPersonId = personId
        }

        override suspend fun clearPersonId() {
            personId.value = null
            savedPersonId = null
        }

        override fun observePersonId(): Flow<String?> = personId
    }

    private class FakeTokenRepository : TokenRepository {
        private var access: String? = null
        private var refresh: String? = null

        override fun saveTokens(accessToken: String, refreshToken: String) {
            access = accessToken
            refresh = refreshToken
        }

        override fun clearTokens() {
            access = null
            refresh = null
        }

        override fun accessToken(): String? = access
        override fun refreshToken(): String? = refresh
    }

    private class FakeBackendAuthRepository(
        private val currentUserResult: Result<AuthenticatedUser> = Result.failure(IllegalStateException("not stubbed")),
        private val requestOtpResult: Result<OtpIssued> = Result.failure(IllegalStateException("not stubbed")),
        private val verifyOtpResult: Result<AuthenticatedUser> = Result.failure(IllegalStateException("not stubbed")),
    ) : BackendAuthRepository {

        override suspend fun register(email: String, password: String, fullName: String): Result<AuthenticatedUser> =
            error("not used by ReceptionAuthViewModel")

        override suspend fun login(email: String, password: String): Result<AuthenticatedUser> =
            error("not used by ReceptionAuthViewModel — Reception App is OTP-only")

        override suspend fun currentUser(): Result<AuthenticatedUser> = currentUserResult

        override suspend fun requestOtp(phoneNumber: String): Result<OtpIssued> = requestOtpResult

        override suspend fun verifyOtp(phoneNumber: String, code: String, fullName: String?): Result<AuthenticatedUser> = verifyOtpResult
    }
}
