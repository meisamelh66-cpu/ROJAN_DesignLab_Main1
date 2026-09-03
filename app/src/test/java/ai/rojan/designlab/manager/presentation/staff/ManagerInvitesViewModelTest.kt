package ai.rojan.designlab.manager.presentation.staff

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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * P1-6, Manager Salon Invite Management: hermetic coverage of
 * [ManagerInvitesViewModel]'s Loading/Error/Ready lifecycle and its
 * create/revoke/QR mutations, mirroring the rigor established by
 * [ManagerSpecialistServicesViewModelTest] (P1-5) — genuine
 * intermediate-state assertions via [CompletableDeferred], real
 * invocation-count checks, no timing-dependent sleeps.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManagerInvitesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleInvite(id: String, status: SalonInviteStatus = SalonInviteStatus.CREATED, role: SalonMemberRole = SalonMemberRole.RECEPTIONIST) = SalonInvite(
        id = id,
        salonId = "salon-1",
        role = role,
        status = status,
        expiresAt = "2026-09-10T00:00:00Z",
        createdAt = "2026-09-03T00:00:00Z",
    )

    /** Constructs a ViewModel whose initial load resolves immediately (synchronously, under [UnconfinedTestDispatcher]) to [ManagerInvitesUiState.Ready]. */
    private fun readyViewModel(repository: FakeManagerInviteRepository): ManagerInvitesViewModel = ManagerInvitesViewModel(
        inviteRepository = { repository },
        initialize = { Result.success(Unit) },
    )

    private fun readyState(viewModel: ManagerInvitesViewModel): ManagerInvitesUiState.Ready =
        viewModel.state as? ManagerInvitesUiState.Ready ?: error("expected Ready state, was ${viewModel.state}")

    @Test
    fun `initial load moves Loading to Ready with the real invite list`() = runTest {
        val initializeCompletion = CompletableDeferred<Result<Unit>>()
        val repository = FakeManagerInviteRepository().apply { listResult = Result.success(listOf(sampleInvite("i1"))) }
        val viewModel = ManagerInvitesViewModel(
            inviteRepository = { repository },
            initialize = { initializeCompletion.await() },
        )

        assertTrue(viewModel.state is ManagerInvitesUiState.Loading)

        initializeCompletion.complete(Result.success(Unit))

        val state = readyState(viewModel)
        assertEquals(1, state.invites.size)
        assertEquals("i1", state.invites.first().id)
    }

    @Test
    fun `initialize failure moves Loading to Error`() = runTest {
        val initializeCompletion = CompletableDeferred<Result<Unit>>()
        val viewModel = ManagerInvitesViewModel(
            inviteRepository = { FakeManagerInviteRepository() },
            initialize = { initializeCompletion.await() },
        )

        assertTrue(viewModel.state is ManagerInvitesUiState.Loading)

        initializeCompletion.complete(Result.failure(IllegalStateException("network down")))

        val state = viewModel.state
        assertTrue(state is ManagerInvitesUiState.Error)
        assertTrue((state as ManagerInvitesUiState.Error).message.isNotBlank())
    }

    @Test
    fun `retry after initial load failure re-invokes initialize and recovers to Ready`() = runTest {
        var initializeCallCount = 0
        var completion = CompletableDeferred<Result<Unit>>()
        val viewModel = ManagerInvitesViewModel(
            inviteRepository = { FakeManagerInviteRepository() },
            initialize = { initializeCallCount++; completion.await() },
        )

        completion.complete(Result.failure(IllegalStateException("network")))
        assertTrue(viewModel.state is ManagerInvitesUiState.Error)
        assertEquals(1, initializeCallCount)

        completion = CompletableDeferred()
        viewModel.retry()

        assertTrue(viewModel.state is ManagerInvitesUiState.Loading)
        assertEquals(2, initializeCallCount)

        completion.complete(Result.success(Unit))
        assertTrue(viewModel.state is ManagerInvitesUiState.Ready)
    }

    @Test
    fun `403 access-denied on initial load surfaces a real user-friendly Error, not a raw exception`() = runTest {
        val repository = FakeManagerInviteRepository().apply {
            listResult = Result.failure(ai.rojan.designlab.data.remote.BackendApiException(403, null))
        }
        val viewModel = readyViewModel(repository)

        val state = viewModel.state
        assertTrue(state is ManagerInvitesUiState.Error)
        assertEquals("اجازه دسترسی به این بخش را ندارید.", (state as ManagerInvitesUiState.Error).message)
    }

    @Test
    fun `create invite success re-fetches the real list rather than locally appending the response`() = runTest {
        val repository = FakeManagerInviteRepository().apply { listResult = Result.success(emptyList()) }
        val viewModel = readyViewModel(repository)

        viewModel.createInvite(SalonMemberRole.RECEPTIONIST)
        assertTrue(readyState(viewModel).isCreating)
        assertEquals(1, repository.createCallCount)
        assertEquals(1, repository.listCallCount) // initial load only so far

        repository.listResult = Result.success(listOf(sampleInvite("i1")))
        repository.completeCreate(Result.success(sampleInvite("i1")))

        val state = readyState(viewModel)
        assertEquals(false, state.isCreating)
        assertEquals(2, repository.listCallCount) // re-fetched after create
        assertEquals(listOf("i1"), state.invites.map { it.id })
        assertNull(state.mutationError)
    }

    @Test
    fun `create invite success preserves a concurrent in-flight revoke's pendingIds and an open QR dialog`() = runTest {
        val repository = FakeManagerInviteRepository().apply {
            listResult = Result.success(listOf(sampleInvite("i1"), sampleInvite("i2")))
        }
        val viewModel = readyViewModel(repository)

        // i2's revoke is left genuinely in flight (never completed) for the rest of this test.
        viewModel.revokeInvite("i2")
        assertEquals(setOf("i2"), readyState(viewModel).pendingIds)

        // i1's QR dialog is open and loaded.
        viewModel.loadQrCode("i1")
        repository.completeQr("i1", Result.success(byteArrayOf(9, 9, 9)))
        val qrCodeBeforeCreate = readyState(viewModel).qrCode
        assertTrue(qrCodeBeforeCreate is QrCodeState.Loaded)

        // A new invite is created concurrently with both of the above still pending.
        viewModel.createInvite(SalonMemberRole.RECEPTIONIST)
        assertTrue(readyState(viewModel).isCreating)

        repository.listResult = Result.success(listOf(sampleInvite("i1"), sampleInvite("i2"), sampleInvite("i3")))
        repository.completeCreate(Result.success(sampleInvite("i3")))

        val state = readyState(viewModel)
        assertEquals(false, state.isCreating)
        assertEquals(listOf("i1", "i2", "i3"), state.invites.map { it.id }) // updated from the real re-fetch
        assertEquals(setOf("i2"), state.pendingIds) // preserved - i2's revoke is still genuinely in flight
        assertEquals(qrCodeBeforeCreate, state.qrCode) // preserved - QR dialog stays open, unaffected by create
    }

    @Test
    fun `create invite failure surfaces mutationError, resets isCreating, and never re-fetches`() = runTest {
        val repository = FakeManagerInviteRepository().apply { listResult = Result.success(emptyList()) }
        val viewModel = readyViewModel(repository)

        viewModel.createInvite(SalonMemberRole.MANAGER)
        repository.completeCreate(Result.failure(IllegalStateException("denied")))

        val state = readyState(viewModel)
        assertEquals(false, state.isCreating)
        assertEquals(1, repository.listCallCount) // no re-fetch after a failed create
        assertTrue(state.invites.isEmpty())
        assertTrue(state.mutationError?.isNotBlank() == true)
    }

    @Test
    fun `revoking a CREATED invite calls revokeInvite then re-fetches the real list`() = runTest {
        val repository = FakeManagerInviteRepository().apply { listResult = Result.success(listOf(sampleInvite("i1", status = SalonInviteStatus.CREATED))) }
        val viewModel = readyViewModel(repository)

        viewModel.revokeInvite("i1")
        assertTrue(readyState(viewModel).pendingIds.contains("i1"))
        assertEquals(1, repository.revokeCallCount)

        repository.listResult = Result.success(listOf(sampleInvite("i1", status = SalonInviteStatus.REVOKED)))
        repository.completeRevoke("i1", Result.success(Unit))

        val state = readyState(viewModel)
        assertTrue(!state.pendingIds.contains("i1"))
        assertEquals(SalonInviteStatus.REVOKED, state.invites.first().status)
    }

    @Test
    fun `revoke failure sets mutationError and leaves the invite list from the last real fetch unchanged`() = runTest {
        val repository = FakeManagerInviteRepository().apply { listResult = Result.success(listOf(sampleInvite("i1"))) }
        val viewModel = readyViewModel(repository)

        viewModel.revokeInvite("i1")
        repository.completeRevoke("i1", Result.failure(IllegalStateException("conflict")))

        val state = readyState(viewModel)
        assertTrue(!state.pendingIds.contains("i1"))
        assertEquals(SalonInviteStatus.CREATED, state.invites.first().status) // unchanged - never optimistically mutated
        assertTrue(state.mutationError?.isNotBlank() == true)
    }

    @Test
    fun `revoke is refused for a non-CREATED invite without calling the repository`() = runTest {
        val repository = FakeManagerInviteRepository().apply {
            listResult = Result.success(listOf(sampleInvite("i1", status = SalonInviteStatus.ACCEPTED)))
        }
        val viewModel = readyViewModel(repository)

        viewModel.revokeInvite("i1")

        assertEquals(0, repository.revokeCallCount)
        assertTrue(readyState(viewModel).pendingIds.isEmpty())
    }

    @Test
    fun `QR fetch is refused for a non-CREATED invite without calling the repository`() = runTest {
        val repository = FakeManagerInviteRepository().apply {
            listResult = Result.success(listOf(sampleInvite("i1", status = SalonInviteStatus.EXPIRED)))
        }
        val viewModel = readyViewModel(repository)

        viewModel.loadQrCode("i1")

        assertEquals(0, repository.qrCallCount)
        assertNull(readyState(viewModel).qrCode)
    }

    @Test
    fun `QR fetch success on a CREATED invite loads the real PNG bytes on demand only`() = runTest {
        val repository = FakeManagerInviteRepository().apply { listResult = Result.success(listOf(sampleInvite("i1"))) }
        val viewModel = readyViewModel(repository)

        assertEquals(0, repository.qrCallCount) // never prefetched at load time

        viewModel.loadQrCode("i1")
        assertTrue(readyState(viewModel).qrCode is QrCodeState.Loading)
        assertEquals(1, repository.qrCallCount)

        val pngBytes = byteArrayOf(1, 2, 3)
        repository.completeQr("i1", Result.success(pngBytes))

        val qrCode = readyState(viewModel).qrCode
        assertTrue(qrCode is QrCodeState.Loaded)
        assertEquals(pngBytes, (qrCode as QrCodeState.Loaded).png)
    }

    @Test
    fun `QR fetch failure surfaces a QrCodeState Error without affecting the rest of Ready state`() = runTest {
        val repository = FakeManagerInviteRepository().apply { listResult = Result.success(listOf(sampleInvite("i1"))) }
        val viewModel = readyViewModel(repository)

        viewModel.loadQrCode("i1")
        repository.completeQr("i1", Result.failure(IllegalStateException("already consumed")))

        val state = readyState(viewModel)
        assertTrue(state.qrCode is QrCodeState.Error)
        assertEquals(1, state.invites.size) // rest of Ready state untouched
    }

    @Test
    fun `dismissQrCode clears the QR dialog state`() = runTest {
        val repository = FakeManagerInviteRepository().apply { listResult = Result.success(listOf(sampleInvite("i1"))) }
        val viewModel = readyViewModel(repository)

        viewModel.loadQrCode("i1")
        repository.completeQr("i1", Result.success(byteArrayOf(1)))
        assertTrue(readyState(viewModel).qrCode != null)

        viewModel.dismissQrCode()

        assertNull(readyState(viewModel).qrCode)
    }

    @Test
    fun `revoking the same invite again while its mutation is in flight does not call revokeInvite twice`() = runTest {
        val repository = FakeManagerInviteRepository().apply { listResult = Result.success(listOf(sampleInvite("i1"))) }
        val viewModel = readyViewModel(repository)

        viewModel.revokeInvite("i1")
        assertEquals(1, repository.revokeCallCount)

        viewModel.revokeInvite("i1")
        assertEquals(1, repository.revokeCallCount)

        repository.completeRevoke("i1", Result.success(Unit))
    }

    private class FakeManagerInviteRepository : ManagerInviteRepository {
        var listResult: Result<List<SalonInvite>> = Result.success(emptyList())

        private val createCompletions = mutableListOf<CompletableDeferred<Result<SalonInvite>>>()
        private val revokeCompletions = mutableMapOf<String, CompletableDeferred<Result<Unit>>>()
        private val qrCompletions = mutableMapOf<String, CompletableDeferred<Result<ByteArray>>>()

        var createCallCount = 0
            private set
        var listCallCount = 0
            private set
        var revokeCallCount = 0
            private set
        var qrCallCount = 0
            private set

        fun completeCreate(result: Result<SalonInvite>) {
            createCompletions.last().complete(result)
        }

        fun completeRevoke(inviteId: String, result: Result<Unit>) {
            revokeCompletions.getOrPut(inviteId) { CompletableDeferred() }.complete(result)
        }

        fun completeQr(inviteId: String, result: Result<ByteArray>) {
            qrCompletions.getOrPut(inviteId) { CompletableDeferred() }.complete(result)
        }

        override suspend fun issueInvite(role: SalonMemberRole): Result<SalonInvite> {
            createCallCount++
            val completion = CompletableDeferred<Result<SalonInvite>>()
            createCompletions += completion
            return completion.await()
        }

        override suspend fun listInvites(): Result<List<SalonInvite>> {
            listCallCount++
            return listResult
        }

        override suspend fun revokeInvite(inviteId: String): Result<Unit> {
            revokeCallCount++
            return revokeCompletions.getOrPut(inviteId) { CompletableDeferred() }.await()
        }

        override suspend fun getInviteQrCode(inviteId: String, sizePx: Int): Result<ByteArray> {
            qrCallCount++
            return qrCompletions.getOrPut(inviteId) { CompletableDeferred() }.await()
        }
    }
}
