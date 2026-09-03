package ai.rojan.designlab.manager.presentation.services

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * Reliability Sprint 01, P0-2: hermetic coverage of
 * [ManagerServicesViewModel]'s Loading/Error/Ready transitions and retry
 * guard — same shape as
 * [ai.rojan.designlab.manager.presentation.dashboard.ManagerDashboardViewModelTest]
 * (P0-1), using an injected `initialize` lambda rather than a fake
 * repository, since there's nothing to fake: [ai.rojan.designlab.manager.data.ManagerRepositories]
 * itself is untouched.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManagerServicesViewModelTest {

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
    fun `initialize success moves Loading to Ready`() = runTest {
        val completion = CompletableDeferred<Result<Unit>>()
        val viewModel = ManagerServicesViewModel(initialize = { completion.await() })

        assertTrue(viewModel.state is ManagerServicesUiState.Loading)

        completion.complete(Result.success(Unit))

        assertEquals(ManagerServicesUiState.Ready, viewModel.state)
    }

    @Test
    fun `initialize failure moves Loading to Error with a real mapped message`() = runTest {
        val completion = CompletableDeferred<Result<Unit>>()
        val viewModel = ManagerServicesViewModel(initialize = { completion.await() })

        assertTrue(viewModel.state is ManagerServicesUiState.Loading)

        completion.complete(Result.failure(IllegalStateException("No active salon selected yet")))

        val state = viewModel.state
        assertTrue(state is ManagerServicesUiState.Error)
        assertTrue((state as ManagerServicesUiState.Error).message.isNotBlank())
    }

    @Test
    fun `retry after failure moves Error to Loading then Ready, and re-invokes initialize`() = runTest {
        var callCount = 0
        var completion = CompletableDeferred<Result<Unit>>()
        val viewModel = ManagerServicesViewModel(
            initialize = {
                callCount++
                completion.await()
            },
        )

        completion.complete(Result.failure(IllegalStateException("network")))
        assertTrue(viewModel.state is ManagerServicesUiState.Error)
        assertEquals(1, callCount)

        completion = CompletableDeferred()
        viewModel.retry()

        assertTrue(viewModel.state is ManagerServicesUiState.Loading)
        assertEquals(2, callCount)

        completion.complete(Result.success(Unit))

        assertEquals(ManagerServicesUiState.Ready, viewModel.state)
    }

    @Test
    fun `retry is a no-op while a load is already in flight`() = runTest {
        var callCount = 0
        val completion = CompletableDeferred<Result<Unit>>()
        val viewModel = ManagerServicesViewModel(
            initialize = {
                callCount++
                completion.await()
            },
        )

        assertTrue(viewModel.state is ManagerServicesUiState.Loading)
        assertEquals(1, callCount)

        viewModel.retry()

        assertEquals(1, callCount)
        assertTrue(viewModel.state is ManagerServicesUiState.Loading)
    }
}
