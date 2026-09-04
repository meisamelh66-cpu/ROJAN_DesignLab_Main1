package ai.rojan.designlab.presentation.booking

import ai.rojan.designlab.domain.booking.RollingBookingDates
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Sprint 5B-8C — [BookingDateViewModel] backs the booking-flow "choose a
 * day" screen. Its job: probe `available-slots` for today and, only when
 * today is full, walk forward through the rolling 7-day window to
 * auto-preselect the first day that has capacity. It is arg-driven (no
 * `SavedStateHandle`): after process death the screen rebuilds it from the
 * restored [BookingViewModel] state and `init { checkAvailability() }`
 * re-probes, so covering `checkAvailability()`/`retry()` *is* the
 * restoration coverage.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookingDateViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** Responds per call index so tests stay independent of the wall-clock dates. */
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

    private val oneSlot = listOf(TimeSlot(start = "2026-09-04T09:00:00", end = "2026-09-04T09:30:00"))

    private fun viewModel(
        repo: AvailabilityRepository,
        salonId: String? = "salon-1",
        specialistId: String? = "spec-1",
        serviceId: String? = "svc-1",
        skipAutoSkip: Boolean = false,
    ) = BookingDateViewModel(salonId, specialistId, serviceId, skipAutoSkip, repo)

    @Test
    fun `skipAutoSkip short-circuits to Success without touching the backend`() = runTest(dispatcher) {
        val repo = ScriptedAvailabilityRepository { _, _ -> Result.success(oneSlot) }
        val vm = viewModel(repo, skipAutoSkip = true)
        advanceUntilIdle()

        assertTrue(vm.state is UiState.Success)
        assertNull(vm.autoSelectedDate)
        assertTrue("a session that already has a date makes no availability probe", repo.datesRequested.isEmpty())
    }

    @Test
    fun `a missing id reports an error and never calls the backend`() = runTest(dispatcher) {
        val repo = ScriptedAvailabilityRepository { _, _ -> Result.success(oneSlot) }
        val vm = viewModel(repo, serviceId = null)
        advanceUntilIdle()

        assertTrue(vm.state is UiState.Error)
        assertTrue(repo.datesRequested.isEmpty())
    }

    @Test
    fun `today has capacity - Success with no auto-preselection and only one probe`() = runTest(dispatcher) {
        val repo = ScriptedAvailabilityRepository { _, _ -> Result.success(oneSlot) }
        val vm = viewModel(repo)
        advanceUntilIdle()

        assertTrue(vm.state is UiState.Success)
        assertNull("today is bookable, nothing to skip to", vm.autoSelectedDate)
        assertEquals(1, repo.datesRequested.size)
    }

    @Test
    fun `today is full - walks forward and auto-selects the first day with capacity`() = runTest(dispatcher) {
        val repo = ScriptedAvailabilityRepository { callIndex, _ ->
            if (callIndex < 2) Result.success(emptyList()) else Result.success(oneSlot)
        }
        val vm = viewModel(repo)
        advanceUntilIdle()

        assertTrue(vm.state is UiState.Success)
        assertEquals("stops at the third probe (today + 2 full days)", 3, repo.datesRequested.size)
        assertEquals(repo.datesRequested[2], vm.autoSelectedDate)
    }

    @Test
    fun `every day in the rolling window is full - Success, no preselection, probes all 7 days`() = runTest(dispatcher) {
        val repo = ScriptedAvailabilityRepository { _, _ -> Result.success(emptyList()) }
        val vm = viewModel(repo)
        advanceUntilIdle()

        assertTrue(vm.state is UiState.Success)
        assertNull(vm.autoSelectedDate)
        assertEquals(RollingBookingDates.next7Days().map { it.first }, repo.datesRequested)
    }

    @Test
    fun `a failure on the first probe resolves to Error`() = runTest(dispatcher) {
        val repo = ScriptedAvailabilityRepository { _, _ -> Result.failure(IOException("availability down")) }
        val vm = viewModel(repo)
        advanceUntilIdle()

        assertTrue(vm.state is UiState.Error)
        assertNull(vm.autoSelectedDate)
    }

    @Test
    fun `a failure on a later probe - after today is full - resolves to Error`() = runTest(dispatcher) {
        val repo = ScriptedAvailabilityRepository { callIndex, _ ->
            when (callIndex) {
                0 -> Result.success(emptyList())
                else -> Result.failure(IOException("availability down"))
            }
        }
        val vm = viewModel(repo)
        advanceUntilIdle()

        assertTrue(vm.state is UiState.Error)
        assertEquals(2, repo.datesRequested.size)
    }

    @Test
    fun `retry re-probes after a failure and can recover to Success`() = runTest(dispatcher) {
        var fail = true
        val repo = ScriptedAvailabilityRepository { _, _ ->
            if (fail) Result.failure(IOException("transient")) else Result.success(oneSlot)
        }
        val vm = viewModel(repo)
        advanceUntilIdle()
        assertTrue(vm.state is UiState.Error)

        fail = false
        vm.retry()
        assertTrue("retry shows Loading immediately", vm.state is UiState.Loading)
        advanceUntilIdle()

        assertTrue(vm.state is UiState.Success)
    }
}
