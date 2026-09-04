package ai.rojan.designlab.presentation.booking

import ai.rojan.designlab.domain.repository.AvailabilityRepository
import ai.rojan.designlab.domain.repository.TimeSlot
import ai.rojan.designlab.presentation.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Sprint 5B-8C — [BookingTimeViewModel] loads the real bookable slots for
 * the chosen day. It is arg-driven (not `SavedStateHandle`-backed): after
 * process death the screen rebuilds it from the restored [BookingViewModel]
 * state and `init { load() }` re-fetches, so covering `load()`/`retry()`
 * *is* the restoration coverage.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookingTimeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class ScriptedAvailabilityRepository(
        private val respond: suspend (callIndex: Int, date: String) -> Result<List<TimeSlot>>,
    ) : AvailabilityRepository {
        val datesRequested = mutableListOf<String>()

        override suspend fun getAvailableSlots(
            salonId: String,
            specialistId: String,
            serviceId: String,
            date: String,
            slotIntervalMinutes: Int,
        ): Result<List<TimeSlot>> {
            val index = datesRequested.size
            datesRequested += date
            return respond(index, date)
        }
    }

    private fun slot(start: String) = TimeSlot(start = start, end = start.dropLast(2) + "30")

    private fun viewModel(
        repo: AvailabilityRepository,
        salonId: String? = "salon-1",
        specialistId: String? = "spec-1",
        serviceId: String? = "svc-1",
        date: String = "2026-09-01",
    ) = BookingTimeViewModel(salonId, specialistId, serviceId, date, repo)

    @Test
    fun `a successful load with slots resolves to Success and passes the date through`() = runTest(dispatcher) {
        val repo = ScriptedAvailabilityRepository { _, _ ->
            Result.success(listOf(slot("2026-09-01T09:00:00"), slot("2026-09-01T09:30:00")))
        }
        val vm = viewModel(repo, date = "2026-09-01")
        advanceUntilIdle()

        val state = vm.state
        assertTrue(state is UiState.Success)
        assertEquals(2, (state as UiState.Success).data.size)
        assertEquals(listOf("2026-09-01"), repo.datesRequested)
    }

    @Test
    fun `an empty slot list resolves to UiState Empty`() = runTest(dispatcher) {
        val vm = viewModel(ScriptedAvailabilityRepository { _, _ -> Result.success(emptyList()) })
        advanceUntilIdle()

        assertTrue(vm.state is UiState.Empty)
    }

    @Test
    fun `a repository failure resolves to UiState Error`() = runTest(dispatcher) {
        val vm = viewModel(ScriptedAvailabilityRepository { _, _ -> Result.failure(IOException("availability down")) })
        advanceUntilIdle()

        assertTrue(vm.state is UiState.Error)
    }

    @Test
    fun `a missing id reports an error and never calls the backend`() = runTest(dispatcher) {
        val repo = ScriptedAvailabilityRepository { _, _ -> Result.success(listOf(slot("2026-09-01T09:00:00"))) }
        val vm = viewModel(repo, specialistId = null)
        advanceUntilIdle()

        assertTrue(vm.state is UiState.Error)
        assertTrue("no availability call for an incomplete booking", repo.datesRequested.isEmpty())
    }

    @Test
    fun `retry re-loads after a failure and can succeed`() = runTest(dispatcher) {
        var fail = true
        val repo = ScriptedAvailabilityRepository { _, _ ->
            if (fail) Result.failure(IOException("transient")) else Result.success(listOf(slot("2026-09-01T09:00:00")))
        }
        val vm = viewModel(repo)
        advanceUntilIdle()
        assertTrue(vm.state is UiState.Error)

        fail = false
        vm.retry()
        assertTrue("retry shows Loading immediately", vm.state is UiState.Loading)
        advanceUntilIdle()

        assertTrue(vm.state is UiState.Success)
        assertEquals(listOf("2026-09-01", "2026-09-01"), repo.datesRequested)
    }
}
