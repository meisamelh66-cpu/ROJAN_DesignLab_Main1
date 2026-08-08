package ai.rojan.designlab.manager.presentation.auth

import ai.rojan.designlab.domain.repository.AuthSessionRepository
import ai.rojan.designlab.domain.repository.AuthenticatedUser
import ai.rojan.designlab.domain.repository.BackendAuthRepository
import ai.rojan.designlab.domain.repository.OtpIssued
import ai.rojan.designlab.domain.repository.TokenRepository
import ai.rojan.designlab.manager.domain.auth.ManagerAuthState
import ai.rojan.designlab.manager.domain.auth.ManagerOtpStep
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

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository)

        assertEquals(ManagerAuthState.Unauthenticated, viewModel.authState.value)
        assertEquals(ManagerOtpStep.EnteringPhone, viewModel.otpStep.value)
    }

    // --- Scenario 2: Existing valid JWT --------------------------------

    @Test
    fun `existing valid session for a manager account restores straight to Authenticated`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository(initialPersonId = managerUser.id)
        val backendAuthRepository = FakeBackendAuthRepository(currentUserResult = Result.success(managerUser))
        val tokenRepository = FakeTokenRepository()

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository)

        val state = viewModel.authState.value
        assertTrue(state is ManagerAuthState.Authenticated)
        assertEquals(managerUser.id, (state as ManagerAuthState.Authenticated).userId)
    }

    @Test
    fun `existing valid session for a non-manager account is treated as unauthenticated and cleared`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository(initialPersonId = customerUser.id)
        val backendAuthRepository = FakeBackendAuthRepository(currentUserResult = Result.success(customerUser))
        val tokenRepository = FakeTokenRepository()

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository)

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

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository)

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

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository)
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
        assertTrue(authSessionRepository.savedRememberMe == true)
    }

    @Test
    fun `verifying an OTP for a non-manager account is rejected without authenticating`() = runTest {
        val authSessionRepository = FakeAuthSessionRepository(initialPersonId = null)
        val backendAuthRepository = FakeBackendAuthRepository(
            requestOtpResult = Result.success(OtpIssued("+989123456789", expiresInSeconds = 120, canResendAfterSeconds = 60)),
            verifyOtpResult = Result.success(customerUser),
        )
        val tokenRepository = FakeTokenRepository()

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository)
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

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository)
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

        val viewModel = ManagerAuthViewModel(authSessionRepository, backendAuthRepository, tokenRepository)
        assertTrue(viewModel.authState.value is ManagerAuthState.Authenticated)

        viewModel.logout()

        assertEquals(ManagerAuthState.Unauthenticated, viewModel.authState.value)
        assertEquals(ManagerOtpStep.EnteringPhone, viewModel.otpStep.value)
        assertNull(tokenRepository.accessToken())
        assertNull(tokenRepository.refreshToken())
        assertEquals(1, authSessionRepository.clearPersonIdCallCount)
    }

    // --- Fakes -----------------------------------------------------------

    private class FakeAuthSessionRepository(initialPersonId: String?) : AuthSessionRepository {
        private val personId = MutableStateFlow(initialPersonId)
        private val rememberMe = MutableStateFlow(true)

        var savedPersonId: String? = null
            private set
        var savedRememberMe: Boolean? = null
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

        override suspend fun saveRememberMe(remember: Boolean) {
            rememberMe.value = remember
            savedRememberMe = remember
        }

        override fun observeRememberMe(): Flow<Boolean> = rememberMe
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

        override suspend fun verifyOtp(phoneNumber: String, code: String): Result<AuthenticatedUser> {
            verifyOtpCall?.invoke(phoneNumber, code)
            return verifyOtpResult
        }
    }
}
