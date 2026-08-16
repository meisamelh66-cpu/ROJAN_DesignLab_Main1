package ai.rojan.designlab.manager.presentation.settings

import ai.rojan.designlab.domain.repository.SalonWorkingHours
import ai.rojan.designlab.domain.repository.TimeInterval
import ai.rojan.designlab.manager.domain.repository.ManagerWorkingHoursRepository
import ai.rojan.designlab.presentation.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Owner Salon Profile Completion (Android-only) — hermetic (no real
 * backend) coverage of [ManagerWorkingHoursViewModel], same fake-repository
 * approach [ManagerSalonSetupViewModelTest] already establishes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManagerWorkingHoursViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun days(state: UiState<List<WorkingDayFormState>>) = (state as UiState.Success).data

    @Test
    fun `load populates all seven days, open where the backend has a record and closed otherwise`() = runTest {
        val repository = FakeManagerWorkingHoursRepository(
            getWorkingHoursResult = Result.success(
                listOf(SalonWorkingHours(dayOfWeek = "SATURDAY", intervals = listOf(TimeInterval("09:00:00", "18:00:00")))),
            ),
        )
        val viewModel = ManagerWorkingHoursViewModel(salonId = "salon-1", repository = repository)

        val loaded = days(viewModel.loadState.value)
        assertEquals(7, loaded.size)
        val saturday = loaded.first { it.dayOfWeek == "SATURDAY" }
        assertTrue(saturday.isOpen)
        assertTrue(saturday.hasExistingRecord)
        assertEquals("09:00:00", saturday.start)
        val sunday = loaded.first { it.dayOfWeek == "SUNDAY" }
        assertFalse(sunday.isOpen)
        assertFalse(sunday.hasExistingRecord)
    }

    @Test
    fun `load flags a day with multiple backend intervals`() = runTest {
        val repository = FakeManagerWorkingHoursRepository(
            getWorkingHoursResult = Result.success(
                listOf(
                    SalonWorkingHours(
                        dayOfWeek = "SATURDAY",
                        intervals = listOf(TimeInterval("09:00:00", "13:00:00"), TimeInterval("16:00:00", "20:00:00")),
                    ),
                ),
            ),
        )
        val viewModel = ManagerWorkingHoursViewModel(salonId = "salon-1", repository = repository)

        val saturday = days(viewModel.loadState.value).first { it.dayOfWeek == "SATURDAY" }
        assertTrue(saturday.hasMultipleIntervals)
        assertTrue(saturday.isOpen)
        // First interval still surfaced for display, even though it can't be safely saved from here.
        assertEquals("09:00:00", saturday.start)
    }

    @Test
    fun `saving an open day with multiple intervals does not call setWorkingHours - data safety`() = runTest {
        val repository = FakeManagerWorkingHoursRepository(
            getWorkingHoursResult = Result.success(
                listOf(
                    SalonWorkingHours(
                        dayOfWeek = "SATURDAY",
                        intervals = listOf(TimeInterval("09:00:00", "13:00:00"), TimeInterval("16:00:00", "20:00:00")),
                    ),
                ),
            ),
        )
        val viewModel = ManagerWorkingHoursViewModel(salonId = "salon-1", repository = repository)

        viewModel.saveDay("SATURDAY")

        assertEquals(0, repository.setWorkingHoursCallCount)
        assertEquals(0, repository.removeWorkingHoursCallCount)
    }

    @Test
    fun `blocked save leaves the multi-interval day's state untouched - no data loss`() = runTest {
        val original = SalonWorkingHours(
            dayOfWeek = "SATURDAY",
            intervals = listOf(TimeInterval("09:00:00", "13:00:00"), TimeInterval("16:00:00", "20:00:00")),
        )
        val repository = FakeManagerWorkingHoursRepository(getWorkingHoursResult = Result.success(listOf(original)))
        val viewModel = ManagerWorkingHoursViewModel(salonId = "salon-1", repository = repository)

        viewModel.saveDay("SATURDAY")

        val saturday = days(viewModel.loadState.value).first { it.dayOfWeek == "SATURDAY" }
        assertTrue(saturday.hasMultipleIntervals)
        assertTrue(saturday.hasExistingRecord)
        assertEquals("09:00:00", saturday.start)
        assertEquals("13:00:00", saturday.end)
        assertFalse(saturday.isSaving)
        assertNull(saturday.error)
    }

    @Test
    fun `closing a multi-interval day is still an allowed explicit delete`() = runTest {
        val repository = FakeManagerWorkingHoursRepository(
            getWorkingHoursResult = Result.success(
                listOf(
                    SalonWorkingHours(
                        dayOfWeek = "SATURDAY",
                        intervals = listOf(TimeInterval("09:00:00", "13:00:00"), TimeInterval("16:00:00", "20:00:00")),
                    ),
                ),
            ),
            removeWorkingHoursResult = Result.success(Unit),
        )
        val viewModel = ManagerWorkingHoursViewModel(salonId = "salon-1", repository = repository)

        viewModel.onToggleOpen("SATURDAY", false)
        viewModel.saveDay("SATURDAY")

        assertEquals(1, repository.removeWorkingHoursCallCount)
        assertEquals(0, repository.setWorkingHoursCallCount)
    }

    @Test
    fun `load failure resolves to Error`() = runTest {
        val viewModel = ManagerWorkingHoursViewModel(
            salonId = "salon-1",
            repository = FakeManagerWorkingHoursRepository(getWorkingHoursResult = Result.failure(RuntimeException("boom"))),
        )

        assertTrue(viewModel.loadState.value is UiState.Error)
    }

    @Test
    fun `null salonId resolves to Error without calling the repository`() = runTest {
        val repository = FakeManagerWorkingHoursRepository(getWorkingHoursResult = Result.success(emptyList()))
        val viewModel = ManagerWorkingHoursViewModel(salonId = null, repository = repository)

        assertTrue(viewModel.loadState.value is UiState.Error)
        assertEquals(0, repository.getWorkingHoursCallCount)
    }

    @Test
    fun `saving an open day calls setWorkingHours with a single interval`() = runTest {
        val repository = FakeManagerWorkingHoursRepository(
            getWorkingHoursResult = Result.success(emptyList()),
            setWorkingHoursResult = Result.success(SalonWorkingHours("SATURDAY", listOf(TimeInterval("09:00:00", "18:00:00")))),
        )
        val viewModel = ManagerWorkingHoursViewModel(salonId = "salon-1", repository = repository)

        viewModel.onToggleOpen("SATURDAY", true)
        viewModel.onStartChange("SATURDAY", "09:00:00")
        viewModel.onEndChange("SATURDAY", "18:00:00")
        viewModel.saveDay("SATURDAY")

        assertEquals(1, repository.setWorkingHoursCallCount)
        assertEquals(0, repository.removeWorkingHoursCallCount)
        assertEquals(listOf(TimeInterval("09:00:00", "18:00:00")), repository.lastSetIntervals)
        val saturday = days(viewModel.loadState.value).first { it.dayOfWeek == "SATURDAY" }
        assertFalse(saturday.isSaving)
        assertNull(saturday.error)
    }

    @Test
    fun `turning off a day that has an existing record calls removeWorkingHours`() = runTest {
        val repository = FakeManagerWorkingHoursRepository(
            getWorkingHoursResult = Result.success(
                listOf(SalonWorkingHours("SATURDAY", listOf(TimeInterval("09:00:00", "18:00:00")))),
            ),
            removeWorkingHoursResult = Result.success(Unit),
        )
        val viewModel = ManagerWorkingHoursViewModel(salonId = "salon-1", repository = repository)

        viewModel.onToggleOpen("SATURDAY", false)
        viewModel.saveDay("SATURDAY")

        assertEquals(1, repository.removeWorkingHoursCallCount)
        assertEquals(0, repository.setWorkingHoursCallCount)
        val saturday = days(viewModel.loadState.value).first { it.dayOfWeek == "SATURDAY" }
        assertFalse(saturday.hasExistingRecord)
    }

    @Test
    fun `turning off a day with no existing record is a local no-op, no network call`() = runTest {
        val repository = FakeManagerWorkingHoursRepository(getWorkingHoursResult = Result.success(emptyList()))
        val viewModel = ManagerWorkingHoursViewModel(salonId = "salon-1", repository = repository)

        viewModel.saveDay("SUNDAY")

        assertEquals(0, repository.removeWorkingHoursCallCount)
        assertEquals(0, repository.setWorkingHoursCallCount)
    }

    @Test
    fun `saving an open day with a blank time field does not call the repository`() = runTest {
        val repository = FakeManagerWorkingHoursRepository(getWorkingHoursResult = Result.success(emptyList()))
        val viewModel = ManagerWorkingHoursViewModel(salonId = "salon-1", repository = repository)

        viewModel.onToggleOpen("SATURDAY", true)
        viewModel.onStartChange("SATURDAY", "09:00:00")
        // end left blank
        viewModel.saveDay("SATURDAY")

        assertEquals(0, repository.setWorkingHoursCallCount)
    }

    @Test
    fun `save failure sets a per-day error and clears isSaving without touching other days`() = runTest {
        val repository = FakeManagerWorkingHoursRepository(
            getWorkingHoursResult = Result.success(emptyList()),
            setWorkingHoursResult = Result.failure(RuntimeException("network down")),
        )
        val viewModel = ManagerWorkingHoursViewModel(salonId = "salon-1", repository = repository)

        viewModel.onToggleOpen("SATURDAY", true)
        viewModel.onStartChange("SATURDAY", "09:00:00")
        viewModel.onEndChange("SATURDAY", "18:00:00")
        viewModel.saveDay("SATURDAY")

        val loaded = days(viewModel.loadState.value)
        val saturday = loaded.first { it.dayOfWeek == "SATURDAY" }
        assertFalse(saturday.isSaving)
        assertTrue(saturday.error != null)
        val sunday = loaded.first { it.dayOfWeek == "SUNDAY" }
        assertNull(sunday.error)
    }

    private class FakeManagerWorkingHoursRepository(
        private val getWorkingHoursResult: Result<List<SalonWorkingHours>>,
        private val setWorkingHoursResult: Result<SalonWorkingHours>? = null,
        private val removeWorkingHoursResult: Result<Unit>? = null,
    ) : ManagerWorkingHoursRepository {

        var getWorkingHoursCallCount = 0
            private set
        var setWorkingHoursCallCount = 0
            private set
        var removeWorkingHoursCallCount = 0
            private set
        var lastSetIntervals: List<TimeInterval>? = null
            private set

        override suspend fun getWorkingHours(salonId: String): Result<List<SalonWorkingHours>> {
            getWorkingHoursCallCount++
            return getWorkingHoursResult
        }

        override suspend fun setWorkingHours(salonId: String, dayOfWeek: String, intervals: List<TimeInterval>): Result<SalonWorkingHours> {
            setWorkingHoursCallCount++
            lastSetIntervals = intervals
            return setWorkingHoursResult ?: error("setWorkingHoursResult not stubbed")
        }

        override suspend fun removeWorkingHours(salonId: String, dayOfWeek: String): Result<Unit> {
            removeWorkingHoursCallCount++
            return removeWorkingHoursResult ?: error("removeWorkingHoursResult not stubbed")
        }
    }
}
