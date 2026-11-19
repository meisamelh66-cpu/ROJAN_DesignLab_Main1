package ai.rojan.designlab.auth

import ai.rojan.designlab.data.identity.DemoIdentityProvider
import ai.rojan.designlab.data.identity.DemoSessionProvider
import ai.rojan.designlab.domain.repository.AuthSessionRepository
import ai.rojan.designlab.domain.repository.AuthenticatedUser
import ai.rojan.designlab.domain.repository.BackendAuthRepository
import ai.rojan.designlab.domain.repository.CurrentUserIdentityContext
import ai.rojan.designlab.domain.repository.CurrentUserIdentityContextRepository
import ai.rojan.designlab.domain.repository.OtpIssued
import ai.rojan.designlab.domain.repository.TokenRepository
import ai.rojan.designlab.presentation.auth.AuthViewModel
import ai.rojan.designlab.screens.auth.AuthScreen
import ai.rojan.designlab.screens.profile.ProfileScreen
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P0-3 Staff Access, source-only continuation: Compose UI coverage for the
 * exact chain a real-device E2E attempt could not safely complete
 * (Profile -> Login -> Phone -> Request OTP -> OTP state -> Verify ->
 * post-verification identity refresh), using the [androidx.compose.ui.platform.testTag]s
 * added alongside this file ("profile_login_row", "auth_phone_field",
 * "auth_request_otp_button", "auth_otp_field", "auth_verify_button") instead
 * of any coordinate estimation - the exact failure mode that caused the
 * device-safety incident this work follows up on.
 *
 * Renders the real [ProfileScreen]/[AuthScreen] composables (not a
 * re-creation, same approach as [AuthScreenScreenshotTest]) against fake
 * repositories - no real backend, no real OTP, no real SMS.
 */
@RunWith(AndroidJUnit4::class)
class CustomerLoginFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val customerUser = AuthenticatedUser(
        id = "user-1",
        email = null,
        phoneNumber = "+989123456789",
        fullName = "Test Customer",
        role = "CUSTOMER",
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

    private fun newAuthViewModel(
        backendAuthRepository: BackendAuthRepository,
        currentUserIdentityContextRepository: CurrentUserIdentityContextRepository = FakeCurrentUserIdentityContextRepository(),
    ): AuthViewModel {
        val identityProvider = DemoIdentityProvider()
        return AuthViewModel(
            sessionProvider = DemoSessionProvider(identityProvider),
            identityProvider = identityProvider,
            authSessionRepository = FakeAuthSessionRepository(),
            backendAuthRepository = backendAuthRepository,
            tokenRepository = FakeTokenRepository(),
            currentUserIdentityContextRepository = currentUserIdentityContextRepository,
        )
    }

    /**
     * Regression test for the exact element involved in the real-device
     * incident: a coordinate-estimated tap on this row (adjacent to
     * "کدهای تخفیف") landed one row off and eventually exited the app
     * entirely. Finding it by [profile_login_row] instead makes that class
     * of mis-tap structurally impossible in this test.
     */
    @Test
    fun tappingTheProfileLoginRowInvokesOnLoginClick() {
        var loginClicked = false
        val authViewModel = newAuthViewModel(backendAuthRepository = FakeBackendAuthRepository())

        composeTestRule.setContent {
            ProfileScreen(
                authViewModel = authViewModel,
                onBackClick = {},
                onBeautyDnaClick = {},
                onAppointmentsClick = {},
                onFollowedSalonsClick = {},
                onFavoritesClick = {},
                onWalletClick = {},
                onCouponsClick = {},
                onMembershipClick = {},
                onLoyaltyClick = {},
                onReviewsClick = {},
                onBeautyTimelineClick = {},
                onLoginClick = { loginClicked = true },
                onLogoutClick = {},
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("profile_login_row").performClick()

        assertTrue("tapping the Login row must invoke onLoginClick", loginClicked)
    }

    /**
     * Drives the full phone -> request OTP -> code entry -> verify chain
     * purely through semantic testTags, then asserts (a) the backend fakes
     * were called with the right arguments, (b) the post-verification
     * callback fired, and (c) the identity-context repository (the real
     * GET /api/v1/users/me/salon-access consumer, [AuthViewModel.refreshIdentityContext])
     * was actually invoked as a result - no real backend, no real OTP.
     */
    @Test
    fun phoneEntryThroughOtpVerifyDrivesRequestOtpStateTransitionAndIdentityRefresh() {
        var authenticatedCallbackFired = false
        val backendAuthRepository = FakeBackendAuthRepository(
            requestOtpResult = Result.success(OtpIssued("+989123456789", expiresInSeconds = 120, canResendAfterSeconds = 60)),
            verifyOtpResult = Result.success(customerUser),
        )
        val identityContextRepository = FakeCurrentUserIdentityContextRepository(Result.success(emptyIdentityContext()))
        val authViewModel = newAuthViewModel(
            backendAuthRepository = backendAuthRepository,
            currentUserIdentityContextRepository = identityContextRepository,
        )

        composeTestRule.setContent {
            AuthScreen(
                authViewModel = authViewModel,
                onBackClick = {},
                onExistingUserAuthenticated = { authenticatedCallbackFired = true },
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("auth_phone_field").performTextInput("09123456789")
        composeTestRule.onNodeWithTag("auth_request_otp_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, backendAuthRepository.requestOtpCallCount)
        // otpStep transitioned to AwaitingCode - the OTP field and the
        // relabelled verify button are now the ones present.
        composeTestRule.onNodeWithTag("auth_otp_field").assertExists()
        composeTestRule.onNodeWithTag("auth_verify_button").assertExists()

        composeTestRule.onNodeWithTag("auth_otp_field").performTextInput("482913")
        composeTestRule.onNodeWithTag("auth_verify_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals("+989123456789", backendAuthRepository.lastVerifyPhoneNumber)
        assertEquals("482913", backendAuthRepository.lastVerifyCode)
        assertTrue("verifyOtp success must fire onExistingUserAuthenticated", authenticatedCallbackFired)
        assertEquals(
            "a successful verify must trigger refreshIdentityContext, i.e. GET /api/v1/users/me/salon-access",
            1,
            identityContextRepository.callCount,
        )
    }

    // --- Fakes (mirroring AuthViewModelTest's style; androidTest has its own source set) ---

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
        override suspend fun savePersonId(personId: String) { this.personId.value = personId }
        override suspend fun clearPersonId() { personId.value = null }
        override fun observePersonId(): Flow<String?> = personId
    }

    private class FakeTokenRepository : TokenRepository {
        private var access: String? = null
        private var refresh: String? = null
        override fun saveTokens(accessToken: String, refreshToken: String) { access = accessToken; refresh = refreshToken }
        override fun clearTokens() { access = null; refresh = null }
        override fun accessToken(): String? = access
        override fun refreshToken(): String? = refresh
    }

    private class FakeBackendAuthRepository(
        private val requestOtpResult: Result<OtpIssued> = Result.failure(IllegalStateException("not stubbed")),
        private val verifyOtpResult: Result<AuthenticatedUser> = Result.failure(IllegalStateException("not stubbed")),
    ) : BackendAuthRepository {
        var requestOtpCallCount = 0
            private set
        var lastVerifyPhoneNumber: String? = null
            private set
        var lastVerifyCode: String? = null
            private set

        override suspend fun register(email: String, password: String, fullName: String): Result<AuthenticatedUser> =
            error("not used by this flow")

        override suspend fun login(email: String, password: String): Result<AuthenticatedUser> =
            error("not used by this flow")

        override suspend fun currentUser(): Result<AuthenticatedUser> =
            error("not used by this flow")

        override suspend fun requestOtp(phoneNumber: String): Result<OtpIssued> {
            requestOtpCallCount++
            return requestOtpResult
        }

        override suspend fun verifyOtp(phoneNumber: String, code: String, fullName: String?): Result<AuthenticatedUser> {
            lastVerifyPhoneNumber = phoneNumber
            lastVerifyCode = code
            return verifyOtpResult
        }
    }
}
