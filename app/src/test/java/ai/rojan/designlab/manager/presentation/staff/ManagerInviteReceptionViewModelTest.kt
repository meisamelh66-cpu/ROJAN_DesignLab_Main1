package ai.rojan.designlab.manager.presentation.staff

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.manager.domain.membership.SalonMemberRole
import ai.rojan.designlab.manager.domain.repository.ManagerInviteRepository
import ai.rojan.designlab.manager.domain.repository.SalonInvite
import ai.rojan.designlab.manager.domain.repository.SalonInviteStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * Gap P0-3-REC-INVITE-UI - hermetic (no real backend) coverage of
 * [ManagerInviteReceptionViewModel], same fake-repository approach every
 * other Manager Settings ViewModel test already establishes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManagerInviteReceptionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts Idle, never calls the repository until createInvite is invoked`() {
        val repository = FakeManagerInviteRepository(issueInviteResult = { _, _ -> error("must not be called before createInvite()") })
        val viewModel = ManagerInviteReceptionViewModel(salonId = "salon-1", repository = repository)

        assertTrue(viewModel.state.value is InviteReceptionUiState.Idle)
    }

    @Test
    fun `createInvite sets Creating immediately, before the repository call resolves`() = runTest {
        val gate = CompletableDeferred<SalonInvite>()
        val repository = FakeManagerInviteRepository(issueInviteResult = { _, _ -> gate.await() })
        val viewModel = ManagerInviteReceptionViewModel(salonId = "salon-1", repository = repository)

        viewModel.createInvite()
        assertTrue(viewModel.state.value is InviteReceptionUiState.Creating)

        gate.complete(SalonInvite("i", "salon-1", SalonMemberRole.RECEPTIONIST, "t", SalonInviteStatus.CREATED, "2026-11-19T10:00:00Z"))
    }

    @Test
    fun `createInvite transitions through Creating then Created on success, with role RECEPTIONIST`() = runTest {
        val repository = FakeManagerInviteRepository(
            issueInviteResult = { _, role ->
                assertEquals(SalonMemberRole.RECEPTIONIST, role)
                SalonInvite(
                    id = "invite-1",
                    salonId = "salon-1",
                    role = SalonMemberRole.RECEPTIONIST,
                    token = "real-token-value",
                    status = SalonInviteStatus.CREATED,
                    expiresAt = "2026-11-19T10:00:00Z",
                )
            },
        )
        val viewModel = ManagerInviteReceptionViewModel(salonId = "salon-1", repository = repository)

        viewModel.createInvite()

        val finalState = viewModel.state.value
        assertTrue(finalState is InviteReceptionUiState.Created)
        val created = (finalState as InviteReceptionUiState.Created).invite
        assertEquals(SalonMemberRole.RECEPTIONIST, created.role)
        assertEquals(SalonInviteStatus.CREATED, created.status)
        assertEquals("2026-11-19T10:00:00Z", created.expiresAt)
        assertEquals("real-token-value", created.token)
    }

    @Test
    fun `createInvite passes the real active salonId through to the repository`() = runTest {
        var capturedSalonId = "not-called"
        val repository = FakeManagerInviteRepository(
            issueInviteResult = { salonId, _ ->
                capturedSalonId = salonId
                SalonInvite("i", salonId, SalonMemberRole.RECEPTIONIST, "t", SalonInviteStatus.CREATED, "2026-11-19T10:00:00Z")
            },
        )
        val viewModel = ManagerInviteReceptionViewModel(salonId = "salon-42", repository = repository)

        viewModel.createInvite()

        assertEquals("salon-42", capturedSalonId)
    }

    @Test
    fun `createInvite emits Error on a generic failure`() = runTest {
        val repository = FakeManagerInviteRepository(issueInviteResult = { _, _ -> throw IOException("network down") })
        val viewModel = ManagerInviteReceptionViewModel(salonId = "salon-1", repository = repository)

        viewModel.createInvite()

        assertTrue(viewModel.state.value is InviteReceptionUiState.Error)
    }

    @Test
    fun `createInvite emits Error when the backend returns 403 Forbidden - caller is not the owner`() = runTest {
        val repository = FakeManagerInviteRepository(
            issueInviteResult = { _, _ -> throw httpException(403) },
        )
        val viewModel = ManagerInviteReceptionViewModel(salonId = "salon-1", repository = repository)

        viewModel.createInvite()

        assertTrue(viewModel.state.value is InviteReceptionUiState.Error)
    }

    @Test
    fun `createInvite emits Error with no active salon, without ever calling the repository`() {
        val repository = FakeManagerInviteRepository(issueInviteResult = { _, _ -> error("must not be called with a null salonId") })
        val viewModel = ManagerInviteReceptionViewModel(salonId = null, repository = repository)

        viewModel.createInvite()

        assertTrue(viewModel.state.value is InviteReceptionUiState.Error)
    }

    private fun httpException(code: Int): HttpException =
        HttpException(Response.error<Any>(code, "{}".toResponseBody("application/json".toMediaType())))
}

/** Scoped to this test file only - [issueInviteResult] throwing by default means an unexpected call fails loudly rather than silently returning a fake success. */
private class FakeManagerInviteRepository(
    private val issueInviteResult: suspend (salonId: String, role: SalonMemberRole) -> SalonInvite = { _, _ -> error("not used by this test") },
) : ManagerInviteRepository {
    override suspend fun issueInvite(salonId: String, role: SalonMemberRole): Result<SalonInvite> =
        try {
            Result.success(issueInviteResult(salonId, role))
        } catch (e: Exception) {
            Result.failure(e)
        }
}
