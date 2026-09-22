package ai.rojan.designlab.presentation.specialist

import ai.rojan.designlab.domain.repository.Specialist
import ai.rojan.designlab.domain.repository.SpecialistRepository
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
 * [SpecialistSelectionViewModel] had no automated test coverage at all,
 * unlike every sibling booking-flow ViewModel (`BookingDateViewModelTest`,
 * `BookingTimeViewModelTest`, `BookingConfirmationViewModelTest`) — found
 * during the file-by-file release audit. Covers its three real states
 * (`Loading` -> `Success`/`Empty`/`Error`) and `retry()`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SpecialistSelectionViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeSpecialistRepository(var result: Result<List<Specialist>>) : SpecialistRepository {
        var getSpecialistsCallCount = 0
        override suspend fun getSpecialists(salonId: String): Result<List<Specialist>> {
            getSpecialistsCallCount++
            return result
        }
        override suspend fun getSpecialist(salonId: String, specialistId: String): Result<Specialist> =
            error("not used by these tests")
    }

    @Test
    fun `a successful non-empty load ends in Success with the backend's specialists`() = runTest(dispatcher) {
        val repo = FakeSpecialistRepository(Result.success(listOf(specialist("spec-1"), specialist("spec-2"))))
        val vm = SpecialistSelectionViewModel("salon-1", repo)

        advanceUntilIdle()

        val state = vm.state
        assertTrue(state is UiState.Success)
        assertEquals(listOf("spec-1", "spec-2"), (state as UiState.Success).data.map { it.id })
    }

    @Test
    fun `a successful empty load ends in Empty, not a false Success`() = runTest(dispatcher) {
        val repo = FakeSpecialistRepository(Result.success(emptyList()))
        val vm = SpecialistSelectionViewModel("salon-1", repo)

        advanceUntilIdle()

        assertEquals(UiState.Empty, vm.state)
    }

    @Test
    fun `a failed load ends in Error with a user-facing message, not a raw exception`() = runTest(dispatcher) {
        val repo = FakeSpecialistRepository(Result.failure(IOException("network down")))
        val vm = SpecialistSelectionViewModel("salon-1", repo)

        advanceUntilIdle()

        val state = vm.state
        assertTrue(state is UiState.Error)
        assertTrue((state as UiState.Error).message.isNotBlank())
    }

    @Test
    fun `retry re-fetches and can recover from a prior failure`() = runTest(dispatcher) {
        val repo = FakeSpecialistRepository(Result.failure(IOException("network down")))
        val vm = SpecialistSelectionViewModel("salon-1", repo)
        advanceUntilIdle()
        assertTrue(vm.state is UiState.Error)

        repo.result = Result.success(listOf(specialist("spec-1")))
        vm.retry()
        advanceUntilIdle()

        assertTrue(vm.state is UiState.Success)
        assertEquals(2, repo.getSpecialistsCallCount)
    }

    private companion object {
        fun specialist(id: String) = Specialist(id = id, salonId = "salon-1", displayName = "Sp $id", bio = null, photoUrl = null)
    }
}
