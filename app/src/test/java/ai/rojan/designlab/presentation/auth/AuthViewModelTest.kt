package ai.rojan.designlab.presentation.auth

import ai.rojan.designlab.data.identity.DemoIdentityProvider
import ai.rojan.designlab.data.identity.DemoSessionProvider
import ai.rojan.designlab.domain.identity.SessionState
import ai.rojan.designlab.domain.repository.AuthSessionRepository
import ai.rojan.designlab.domain.repository.AuthenticatedUser
import ai.rojan.designlab.domain.repository.BackendAuthRepository
import ai.rojan.designlab.domain.repository.CurrentUserIdentityContext
import ai.rojan.designlab.domain.repository.CurrentUserIdentityContextRepository
import ai.rojan.designlab.domain.repository.OtpIssued
import ai.rojan.designlab.domain.repository.TokenRepository
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
 * Customer Authentication Migration — hermetic (no real backend, no
 * Android framework) coverage of [AuthViewModel]'s OTP/session/logout
 * logic, mirroring [ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModelTest]'s
 * fake-repository approach. [sessionProvider]/[identityProvider] use the
 * real [DemoSessionProvider]/[DemoIdentityProvider] (same construction
 * [AuthViewModelFactory] itself uses) rather than hand-rolled fakes —
 * they're already pure in-memory Kotlin with no Android dependency, and
 * nothing here tests the (separate, demo-only) business-login/staff-role
 * behavior they otherwise back.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val customerUser = AuthenticatedUser(id = "user-1", email = null, phoneNumber = "+989123456789", fullName = "Test Customer", role = "CUSTOMER")

    private fun newViewModel(
        authSessionRepository: AuthSessionRepository = FakeAuthSessionRepository(),
        backendAuthRepository: BackendAuthRepository = FakeBackendAuthRepository(),
        tokenRepository: TokenRepository = FakeTokenRepository(),
        currentUserIdentityContextRepository: CurrentUserIdentityContextRepository = FakeCurrentUserIdentityContextRepository(),
    ) = AuthViewModel(
        sessionProvider = DemoSessionProvider(DemoIdentityProvider()),
        identityProvider = DemoIdentityProvider(),
        authSessionRepository = authSessionRepository,
        backendAuthRepository = backendAuthRepository,
        tokenRepository = tokenRepository,
        currentUserIdentityContextRepository = currentUserIdentityContextRepository,
    )

    private fun emptyIdentityContext() = CurrentUserIdentityContext(
        userId = customerUser.id,
        phoneNumber = customerUser.phoneNumber,
        email = customerUser.email,
        fullName = customerUser.fullName,
        globalRole = customerUser.role,
        ownedSalons = emptyList(),
        memberships = emptyList(),
        specialistLinks = emptyList(),
    )

    @Test
    fun `fresh install lands on the phone entry step, logged out`() = runTest {
        val viewModel = newViewModel()

        assertEquals(SessionState.LoggedOut, viewModel.sessionState.value)
        assertEquals(CustomerOtpStep.EnteringPhone, viewModel.otpStep.value)
    }

    @Test
    fun `requesting an OTP for a blank phone number shows an error and does not call the backend`() = runTest {
        val backendAuthRepository = FakeBackendAuthRepository()
        val viewModel = newViewModel(backendAuthRepository = backendAuthRepository)

        viewModel.requestOtp("   ")

        assertEquals(CustomerOtpStep.EnteringPhone, viewModel.otpStep.value)
        assertTrue(viewModel.errorMessage.value != null)
        assertEquals(0, backendAuthRepository.requestOtpCallCount)
    }

    @Test
    fun `requesting an OTP moves to the awaiting-code step`() = runTest {
        val backendAuthRepository = FakeBackendAuthRepository(
            requestOtpResult = Result.success(OtpIssued("+989123456789", expiresInSeconds = 120, canResendAfterSeconds = 60)),
        )
        val viewModel = newViewModel(backendAuthRepository = backendAuthRepository)

        viewModel.requestOtp("+989123456789")

        val step = viewModel.otpStep.value
        assertTrue(step is CustomerOtpStep.AwaitingCode)
        assertEquals("+989123456789", (step as CustomerOtpStep.AwaitingCode).phoneNumber)
    }

    @Test
    fun `verifying a correct OTP authenticates and persists the session`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository()
        val backendAuthRepository = FakeBackendAuthRepository(
            requestOtpResult = Result.success(OtpIssued("+989123456789", expiresInSeconds = 120, canResendAfterSeconds = 60)),
            verifyOtpResult = Result.success(customerUser),
        )
        val viewModel = newViewModel(authSessionRepository = authSessionRepository, backendAuthRepository = backendAuthRepository)

        viewModel.requestOtp("+989123456789")
        backendAuthRepository.verifyOtpCall = { phoneNumber, code, fullName ->
            assertEquals("+989123456789", phoneNumber)
            assertEquals("482913", code)
            assertEquals("رویا", fullName)
        }
        viewModel.verifyOtp("482913", "رویا")

        assertEquals(SessionState.LoggedIn(customerUser.id), viewModel.sessionState.value)
        assertEquals(customerUser, viewModel.currentUser.value)
        assertEquals(customerUser.id, authSessionRepository.savedPersonId)
    }

    @Test
    fun `an empty name is not sent to the backend`() = runTest {
        val backendAuthRepository = FakeBackendAuthRepository(
            requestOtpResult = Result.success(OtpIssued("+989123456789", expiresInSeconds = 120, canResendAfterSeconds = 60)),
            verifyOtpResult = Result.success(customerUser),
        )
        val viewModel = newViewModel(backendAuthRepository = backendAuthRepository)

        viewModel.requestOtp("+989123456789")
        backendAuthRepository.verifyOtpCall = { _, _, fullName -> assertNull(fullName) }
        viewModel.verifyOtp("482913", "   ")
    }

    @Test
    fun `a wrong OTP code stays on the awaiting-code step with an error, unauthenticated`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository()
        val backendAuthRepository = FakeBackendAuthRepository(
            requestOtpResult = Result.success(OtpIssued("+989123456789", expiresInSeconds = 120, canResendAfterSeconds = 60)),
            verifyOtpResult = Result.failure(IllegalStateException("401 - invalid or expired code")),
        )
        val viewModel = newViewModel(authSessionRepository = authSessionRepository, backendAuthRepository = backendAuthRepository)

        viewModel.requestOtp("+989123456789")
        viewModel.verifyOtp("000000")

        assertEquals(SessionState.LoggedOut, viewModel.sessionState.value)
        assertTrue(viewModel.otpStep.value is CustomerOtpStep.AwaitingCode)
        assertTrue(viewModel.errorMessage.value != null)
        assertNull(authSessionRepository.savedPersonId)
    }

    @Test
    fun `resend requests a fresh code for the same phone number`() = runTest {
        val backendAuthRepository = FakeBackendAuthRepository(
            requestOtpResult = Result.success(OtpIssued("+989123456789", expiresInSeconds = 120, canResendAfterSeconds = 60)),
        )
        val viewModel = newViewModel(backendAuthRepository = backendAuthRepository)

        viewModel.requestOtp("+989123456789")
        viewModel.resendOtp()

        assertEquals(2, backendAuthRepository.requestOtpCallCount)
        assertTrue(viewModel.otpStep.value is CustomerOtpStep.AwaitingCode)
    }

    @Test
    fun `editing the phone number returns to the entry step and clears the error`() = runTest {
        val backendAuthRepository = FakeBackendAuthRepository(
            requestOtpResult = Result.success(OtpIssued("+989123456789", expiresInSeconds = 120, canResendAfterSeconds = 60)),
            verifyOtpResult = Result.failure(IllegalStateException("401 - invalid or expired code")),
        )
        val viewModel = newViewModel(backendAuthRepository = backendAuthRepository)
        viewModel.requestOtp("+989123456789")
        viewModel.verifyOtp("000000")
        assertTrue(viewModel.errorMessage.value != null)

        viewModel.editPhoneNumber()

        assertEquals(CustomerOtpStep.EnteringPhone, viewModel.otpStep.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `an existing valid session restores straight to logged in`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository()
        val backendAuthRepository = FakeBackendAuthRepository(currentUserResult = Result.success(customerUser))
        val viewModel = newViewModel(authSessionRepository = authSessionRepository, backendAuthRepository = backendAuthRepository)

        viewModel.restoreSession(customerUser.id)

        assertEquals(SessionState.LoggedIn(customerUser.id), viewModel.sessionState.value)
        assertEquals(customerUser, viewModel.currentUser.value)
    }

    @Test
    fun `an expired or revoked session fails restoration and clears the stale session`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository()
        val tokenRepository = FakeTokenRepository()
        tokenRepository.saveTokens("stale-access", "stale-refresh")
        val backendAuthRepository = FakeBackendAuthRepository(
            currentUserResult = Result.failure(IllegalStateException("401 - refresh token also invalid")),
        )
        val viewModel = newViewModel(authSessionRepository = authSessionRepository, backendAuthRepository = backendAuthRepository, tokenRepository = tokenRepository)

        viewModel.restoreSession("some-persisted-id")

        assertEquals(SessionState.LoggedOut, viewModel.sessionState.value)
        assertNull(tokenRepository.accessToken())
        assertNull(tokenRepository.refreshToken())
        assertEquals(1, authSessionRepository.clearPersonIdCallCount)
    }

    @Test
    fun `logout clears the session and returns to the phone entry step`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository()
        val backendAuthRepository = FakeBackendAuthRepository(
            requestOtpResult = Result.success(OtpIssued("+989123456789", expiresInSeconds = 120, canResendAfterSeconds = 60)),
            verifyOtpResult = Result.success(customerUser),
        )
        val tokenRepository = FakeTokenRepository()
        tokenRepository.saveTokens("access", "refresh")
        val viewModel = newViewModel(authSessionRepository = authSessionRepository, backendAuthRepository = backendAuthRepository, tokenRepository = tokenRepository)
        viewModel.requestOtp("+989123456789")
        viewModel.verifyOtp("482913")
        assertTrue(viewModel.sessionState.value is SessionState.LoggedIn)

        viewModel.logout()

        assertEquals(SessionState.LoggedOut, viewModel.sessionState.value)
        assertEquals(CustomerOtpStep.EnteringPhone, viewModel.otpStep.value)
        assertNull(tokenRepository.accessToken())
        assertNull(tokenRepository.refreshToken())
        assertEquals(1, authSessionRepository.clearPersonIdCallCount)
    }

    @Test
    fun `salon-access is fetched after a successful OTP verification, empty lists are a valid success`() = runTest {
        val identityContextRepository = FakeCurrentUserIdentityContextRepository(Result.success(emptyIdentityContext()))
        val backendAuthRepository = FakeBackendAuthRepository(
            requestOtpResult = Result.success(OtpIssued("+989123456789", expiresInSeconds = 120, canResendAfterSeconds = 60)),
            verifyOtpResult = Result.success(customerUser),
        )
        val viewModel = newViewModel(backendAuthRepository = backendAuthRepository, currentUserIdentityContextRepository = identityContextRepository)

        viewModel.requestOtp("+989123456789")
        viewModel.verifyOtp("482913")

        assertEquals(1, identityContextRepository.callCount)
        val state = viewModel.identityContext.value
        assertTrue(state is UiState.Success<CurrentUserIdentityContext>)
        assertTrue((state as UiState.Success<CurrentUserIdentityContext>).data.ownedSalons.isEmpty())
        assertTrue(state.data.memberships.isEmpty())
        assertTrue(state.data.specialistLinks.isEmpty())
    }

    @Test
    fun `a salon-access failure does not create fake permissions and does not roll back the authenticated session`() = runTest {
        val identityContextRepository = FakeCurrentUserIdentityContextRepository(Result.failure(IllegalStateException("network down")))
        val backendAuthRepository = FakeBackendAuthRepository(
            requestOtpResult = Result.success(OtpIssued("+989123456789", expiresInSeconds = 120, canResendAfterSeconds = 60)),
            verifyOtpResult = Result.success(customerUser),
        )
        val viewModel = newViewModel(backendAuthRepository = backendAuthRepository, currentUserIdentityContextRepository = identityContextRepository)

        viewModel.requestOtp("+989123456789")
        viewModel.verifyOtp("482913")

        assertTrue(viewModel.identityContext.value is UiState.Error)
        // The real OTP session must survive a salon-access failure - it's additive, not a replacement.
        assertEquals(SessionState.LoggedIn(customerUser.id), viewModel.sessionState.value)
        assertEquals(customerUser, viewModel.currentUser.value)
    }

    @Test
    fun `session restore re-fetches a fresh salon-access context`() = runTest {
        val identityContextRepository = FakeCurrentUserIdentityContextRepository(Result.success(emptyIdentityContext()))
        val backendAuthRepository = FakeBackendAuthRepository(currentUserResult = Result.success(customerUser))
        val viewModel = newViewModel(backendAuthRepository = backendAuthRepository, currentUserIdentityContextRepository = identityContextRepository)

        viewModel.restoreSession(customerUser.id)

        assertEquals(1, identityContextRepository.callCount)
        assertTrue(viewModel.identityContext.value is UiState.Success<CurrentUserIdentityContext>)
    }

    @Test
    fun `logout resets the identity context back to loading`() = runTest {
        val identityContextRepository = FakeCurrentUserIdentityContextRepository(Result.success(emptyIdentityContext()))
        val backendAuthRepository = FakeBackendAuthRepository(
            requestOtpResult = Result.success(OtpIssued("+989123456789", expiresInSeconds = 120, canResendAfterSeconds = 60)),
            verifyOtpResult = Result.success(customerUser),
        )
        val viewModel = newViewModel(backendAuthRepository = backendAuthRepository, currentUserIdentityContextRepository = identityContextRepository)
        viewModel.requestOtp("+989123456789")
        viewModel.verifyOtp("482913")
        assertTrue(viewModel.identityContext.value is UiState.Success<CurrentUserIdentityContext>)

        viewModel.logout()

        assertEquals(UiState.Loading, viewModel.identityContext.value)
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

    private class FakeAuthSessionRepository : AuthSessionRepository {
        private val personId = MutableStateFlow<String?>(null)

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

        var verifyOtpCall: ((phoneNumber: String, code: String, fullName: String?) -> Unit)? = null
        var requestOtpCallCount = 0
            private set

        override suspend fun register(email: String, password: String, fullName: String): Result<AuthenticatedUser> =
            error("not used by AuthViewModel — Customer App is OTP-only")

        override suspend fun login(email: String, password: String): Result<AuthenticatedUser> =
            error("not used by AuthViewModel — Customer App is OTP-only")

        override suspend fun currentUser(): Result<AuthenticatedUser> = currentUserResult

        override suspend fun requestOtp(phoneNumber: String): Result<OtpIssued> {
            requestOtpCallCount++
            return requestOtpResult
        }

        override suspend fun verifyOtp(phoneNumber: String, code: String, fullName: String?): Result<AuthenticatedUser> {
            verifyOtpCall?.invoke(phoneNumber, code, fullName)
            return verifyOtpResult
        }
    }
}
