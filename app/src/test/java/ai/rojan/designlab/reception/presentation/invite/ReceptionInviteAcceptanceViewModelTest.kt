package ai.rojan.designlab.reception.presentation.invite

import ai.rojan.designlab.domain.repository.AuthenticatedUser
import ai.rojan.designlab.domain.repository.CurrentUserIdentityContext
import ai.rojan.designlab.domain.repository.OtpIssued
import ai.rojan.designlab.domain.repository.SalonMembershipAccess
import ai.rojan.designlab.reception.domain.repository.SalonInviteAcceptance
import ai.rojan.designlab.reception.domain.repository.SalonInvitePreview
import kotlinx.coroutines.CompletableDeferred
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
 * P1-1, Reception Invite Acceptance Flow: hermetic coverage of
 * [ReceptionInviteAcceptanceViewModel]'s Loading/Error/Preview/Accepted
 * lifecycle, its independent OTP sub-flow, and the
 * accept -> refresh identity -> resolve salon sequence — same
 * [CompletableDeferred]-based genuine-intermediate-state rigor established
 * by P0-1/P0-2/P0-3/P1-5. Every backend call is a plain injected lambda
 * (this ViewModel's own constructor shape), so no hand-written fake
 * repository class is needed here at all.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReceptionInviteAcceptanceViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val token = "test-token"

    private fun identityContext(salonId: String = "salon-1", salonName: String = "سالن نمونه") = CurrentUserIdentityContext(
        userId = "user-1",
        phoneNumber = "+989123456789",
        email = null,
        fullName = "پذیرش تست",
        globalRole = "CUSTOMER",
        ownedSalons = emptyList(),
        memberships = listOf(
            SalonMembershipAccess(
                membershipId = "membership-1",
                salonId = salonId,
                salonName = salonName,
                active = true,
                role = "RECEPTIONIST",
                permissions = setOf("MANAGE_BOOKINGS"),
            ),
        ),
        specialistLinks = emptyList(),
    )

    /** Builds a ViewModel whose invite fetch resolves immediately to [ReceptionInviteAcceptanceUiState.Preview]. */
    private fun previewViewModel(
        acceptInvite: suspend (String) -> Result<SalonInviteAcceptance> = { error("not used") },
        getIdentityContext: suspend () -> Result<CurrentUserIdentityContext> = { error("not used") },
        saveActiveSalonId: suspend (String) -> Unit = { },
        savePersonId: suspend (String) -> Unit = { },
        verifyOtp: suspend (String, String) -> Result<AuthenticatedUser> = { _, _ -> error("not used") },
    ): ReceptionInviteAcceptanceViewModel = ReceptionInviteAcceptanceViewModel(
        token = token,
        getInvite = { Result.success(SalonInvitePreview(salonName = "سالن نمونه", role = "RECEPTIONIST")) },
        requestOtp = { phone -> Result.success(OtpIssued(phoneNumber = phone, expiresInSeconds = 120, canResendAfterSeconds = 30)) },
        verifyOtp = verifyOtp,
        acceptInvite = acceptInvite,
        getIdentityContext = getIdentityContext,
        saveActiveSalonId = saveActiveSalonId,
        savePersonId = savePersonId,
    )

    private fun previewState(viewModel: ReceptionInviteAcceptanceViewModel): ReceptionInviteAcceptanceUiState.Preview =
        viewModel.state as? ReceptionInviteAcceptanceUiState.Preview ?: error("expected Preview state, was ${viewModel.state}")

    @Test
    fun `initial load moves Loading to Preview with the real salon name and role`() = runTest {
        val completion = CompletableDeferred<Result<SalonInvitePreview>>()
        val viewModel = ReceptionInviteAcceptanceViewModel(
            token = token,
            getInvite = { completion.await() },
            requestOtp = { error("not used") },
            verifyOtp = { _, _ -> error("not used") },
            acceptInvite = { error("not used") },
            getIdentityContext = { error("not used") },
            saveActiveSalonId = { },
            savePersonId = { },
        )

        assertTrue(viewModel.state is ReceptionInviteAcceptanceUiState.Loading)

        completion.complete(Result.success(SalonInvitePreview(salonName = "سالن الف", role = "MANAGER")))

        val state = previewState(viewModel)
        assertEquals("سالن الف", state.salonName)
        assertEquals("MANAGER", state.role)
    }

    @Test
    fun `initial load failure moves Loading to Error`() = runTest {
        val completion = CompletableDeferred<Result<SalonInvitePreview>>()
        val viewModel = ReceptionInviteAcceptanceViewModel(
            token = token,
            getInvite = { completion.await() },
            requestOtp = { error("not used") },
            verifyOtp = { _, _ -> error("not used") },
            acceptInvite = { error("not used") },
            getIdentityContext = { error("not used") },
            saveActiveSalonId = { },
            savePersonId = { },
        )

        completion.complete(Result.failure(IllegalStateException("not found")))

        val state = viewModel.state
        assertTrue(state is ReceptionInviteAcceptanceUiState.Error)
        assertTrue((state as ReceptionInviteAcceptanceUiState.Error).message.isNotBlank())
    }

    @Test
    fun `retry after load failure re-invokes getInvite and recovers to Preview`() = runTest {
        var callCount = 0
        var completion = CompletableDeferred<Result<SalonInvitePreview>>()
        val viewModel = ReceptionInviteAcceptanceViewModel(
            token = token,
            getInvite = { callCount++; completion.await() },
            requestOtp = { error("not used") },
            verifyOtp = { _, _ -> error("not used") },
            acceptInvite = { error("not used") },
            getIdentityContext = { error("not used") },
            saveActiveSalonId = { },
            savePersonId = { },
        )

        completion.complete(Result.failure(IllegalStateException("network")))
        assertTrue(viewModel.state is ReceptionInviteAcceptanceUiState.Error)
        assertEquals(1, callCount)

        completion = CompletableDeferred()
        viewModel.retry()

        assertTrue(viewModel.state is ReceptionInviteAcceptanceUiState.Loading)
        assertEquals(2, callCount)

        completion.complete(Result.success(SalonInvitePreview(salonName = "سالن", role = "RECEPTIONIST")))
        assertTrue(viewModel.state is ReceptionInviteAcceptanceUiState.Preview)
    }

    @Test
    fun `requestOtpCode success moves otpStep to AwaitingCode`() = runTest {
        val viewModel = previewViewModel()

        viewModel.requestOtpCode("09123456789")

        val step = viewModel.otpStep.value
        assertTrue(step is ReceptionInviteOtpStep.AwaitingCode)
        assertEquals(false, viewModel.isSubmitting.value)
    }

    @Test
    fun `requestOtpCode failure sets authErrorMessage and leaves otpStep at EnteringPhone`() = runTest {
        var attempted = false
        val viewModel = ReceptionInviteAcceptanceViewModel(
            token = token,
            getInvite = { Result.success(SalonInvitePreview(salonName = "سالن", role = "RECEPTIONIST")) },
            requestOtp = { attempted = true; Result.failure(IllegalStateException("rate limited")) },
            verifyOtp = { _, _ -> error("not used") },
            acceptInvite = { error("not used") },
            getIdentityContext = { error("not used") },
            saveActiveSalonId = { },
            savePersonId = { },
        )

        viewModel.requestOtpCode("09123456789")

        assertTrue(attempted)
        assertTrue(viewModel.otpStep.value is ReceptionInviteOtpStep.EnteringPhone)
        assertTrue(viewModel.authErrorMessage.value?.isNotBlank() == true)
    }

    @Test
    fun `verifyOtpCode success runs accept then identity refresh then resolves the accepted salon into Accepted`() = runTest {
        var savedPersonId: String? = null
        var savedActiveSalonId: String? = null

        val viewModel = previewViewModel(
            verifyOtp = { _, _ -> Result.success(AuthenticatedUser(id = "user-1", email = null, phoneNumber = "+989123456789", fullName = "پذیرش تست", role = "CUSTOMER")) },
            acceptInvite = { Result.success(SalonInviteAcceptance(salonId = "salon-1", role = "RECEPTIONIST")) },
            getIdentityContext = { Result.success(identityContext(salonId = "salon-1", salonName = "سالن نمونه")) },
            saveActiveSalonId = { savedActiveSalonId = it },
            savePersonId = { savedPersonId = it },
        )

        viewModel.requestOtpCode("09123456789")
        viewModel.verifyOtpCode("1234")

        val state = viewModel.state
        assertTrue(state is ReceptionInviteAcceptanceUiState.Accepted)
        state as ReceptionInviteAcceptanceUiState.Accepted
        assertEquals("salon-1", state.salonId)
        assertEquals("سالن نمونه", state.salonName)
        assertEquals("پذیرش تست", state.fullName)
        assertEquals("+989123456789", state.phoneNumber)
        assertEquals("salon-1", savedActiveSalonId)
        assertEquals("user-1", savedPersonId)
        assertEquals(false, viewModel.isSubmitting.value)
        assertNull(viewModel.authErrorMessage.value)
    }

    @Test
    fun `verifyOtpCode failure sets authErrorMessage, resets isSubmitting, and never reaches Accepted`() = runTest {
        val viewModel = previewViewModel(
            verifyOtp = { _, _ -> Result.failure(IllegalStateException("invalid code")) },
        )

        viewModel.requestOtpCode("09123456789")
        viewModel.verifyOtpCode("0000")

        assertTrue(viewModel.state is ReceptionInviteAcceptanceUiState.Preview)
        assertEquals(false, viewModel.isSubmitting.value)
        assertTrue(viewModel.authErrorMessage.value?.isNotBlank() == true)
    }

    @Test
    fun `acceptInvite failure after a successful OTP verify sets authErrorMessage and never reaches Accepted`() = runTest {
        val viewModel = previewViewModel(
            verifyOtp = { _, _ -> Result.success(AuthenticatedUser(id = "user-1", email = null, phoneNumber = "+989123456789", fullName = "پذیرش تست", role = "CUSTOMER")) },
            acceptInvite = { Result.failure(IllegalStateException("invite already accepted")) },
        )

        viewModel.requestOtpCode("09123456789")
        viewModel.verifyOtpCode("1234")

        assertTrue(viewModel.state is ReceptionInviteAcceptanceUiState.Preview)
        assertEquals(false, viewModel.isSubmitting.value)
        assertTrue(viewModel.authErrorMessage.value?.isNotBlank() == true)
    }

    @Test
    fun `retrying after an identity refresh failure does not re-consume the single-use invite token`() = runTest {
        var acceptCallCount = 0
        var identityCallCount = 0
        var identityShouldFail = true

        val viewModel = previewViewModel(
            verifyOtp = { _, _ -> Result.success(AuthenticatedUser(id = "user-1", email = null, phoneNumber = "+989123456789", fullName = "پذیرش تست", role = "CUSTOMER")) },
            acceptInvite = { acceptCallCount++; Result.success(SalonInviteAcceptance(salonId = "salon-1", role = "RECEPTIONIST")) },
            getIdentityContext = {
                identityCallCount++
                if (identityShouldFail) Result.failure(IllegalStateException("network down")) else Result.success(identityContext(salonId = "salon-1"))
            },
        )

        viewModel.requestOtpCode("09123456789")
        viewModel.verifyOtpCode("1234")

        assertEquals(1, acceptCallCount)
        assertEquals(1, identityCallCount)
        assertTrue(viewModel.state is ReceptionInviteAcceptanceUiState.Preview)

        // User resends a fresh code and retries — acceptInvite(token) must
        // NOT be called a second time (the token is already consumed on the
        // backend by the first, successful call above).
        identityShouldFail = false
        viewModel.resendOtpCode()
        viewModel.verifyOtpCode("5678")

        assertEquals(1, acceptCallCount)
        assertEquals(2, identityCallCount)
        assertTrue(viewModel.state is ReceptionInviteAcceptanceUiState.Accepted)
    }

    @Test
    fun `identity refresh failure after a successful accept sets authErrorMessage and never reaches Accepted`() = runTest {
        val viewModel = previewViewModel(
            verifyOtp = { _, _ -> Result.success(AuthenticatedUser(id = "user-1", email = null, phoneNumber = "+989123456789", fullName = "پذیرش تست", role = "CUSTOMER")) },
            acceptInvite = { Result.success(SalonInviteAcceptance(salonId = "salon-1", role = "RECEPTIONIST")) },
            getIdentityContext = { Result.failure(IllegalStateException("network down")) },
        )

        viewModel.requestOtpCode("09123456789")
        viewModel.verifyOtpCode("1234")

        assertTrue(viewModel.state is ReceptionInviteAcceptanceUiState.Preview)
        assertEquals(false, viewModel.isSubmitting.value)
        assertTrue(viewModel.authErrorMessage.value?.isNotBlank() == true)
    }

    @Test
    fun `accepted salon missing from the fresh identity context sets authErrorMessage instead of a fabricated Accepted state`() = runTest {
        val viewModel = previewViewModel(
            verifyOtp = { _, _ -> Result.success(AuthenticatedUser(id = "user-1", email = null, phoneNumber = "+989123456789", fullName = "پذیرش تست", role = "CUSTOMER")) },
            acceptInvite = { Result.success(SalonInviteAcceptance(salonId = "salon-1", role = "RECEPTIONIST")) },
            getIdentityContext = { Result.success(identityContext(salonId = "some-other-salon")) },
        )

        viewModel.requestOtpCode("09123456789")
        viewModel.verifyOtpCode("1234")

        assertTrue(viewModel.state is ReceptionInviteAcceptanceUiState.Preview)
        assertEquals(false, viewModel.isSubmitting.value)
        assertTrue(viewModel.authErrorMessage.value?.isNotBlank() == true)
    }

    @Test
    fun `resendOtpCode reuses the phone number already awaiting a code`() = runTest {
        var requestedPhone: String? = null
        val viewModel = ReceptionInviteAcceptanceViewModel(
            token = token,
            getInvite = { Result.success(SalonInvitePreview(salonName = "سالن", role = "RECEPTIONIST")) },
            requestOtp = { phone -> requestedPhone = phone; Result.success(OtpIssued(phoneNumber = phone, expiresInSeconds = 120, canResendAfterSeconds = 30)) },
            verifyOtp = { _, _ -> error("not used") },
            acceptInvite = { error("not used") },
            getIdentityContext = { error("not used") },
            saveActiveSalonId = { },
            savePersonId = { },
        )

        viewModel.requestOtpCode("09123456789")
        requestedPhone = null
        viewModel.resendOtpCode()

        assertEquals("+989123456789", requestedPhone)
    }

    @Test
    fun `editPhoneNumber resets otpStep back to EnteringPhone`() = runTest {
        val viewModel = previewViewModel()

        viewModel.requestOtpCode("09123456789")
        assertTrue(viewModel.otpStep.value is ReceptionInviteOtpStep.AwaitingCode)

        viewModel.editPhoneNumber()

        assertTrue(viewModel.otpStep.value is ReceptionInviteOtpStep.EnteringPhone)
    }
}
