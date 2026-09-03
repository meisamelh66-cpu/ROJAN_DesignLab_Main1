package ai.rojan.designlab.presentation.booking

import ai.rojan.designlab.domain.repository.AvailabilityRepository
import ai.rojan.designlab.domain.repository.Booking
import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.domain.repository.PagedResult
import ai.rojan.designlab.domain.repository.TimeSlot
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
 * Reliability Sprint 01, P0-3: hermetic coverage of [RescheduleViewModel.confirm]
 * — success, failure (state/selection preserved), and the double-submit
 * guard added alongside this test (`if (current.isSubmitting) return`,
 * mirroring [BookingConfirmationViewModel.confirmBooking]'s existing one).
 * Same [CompletableDeferred]-based in-flight-observation idiom as
 * [BookingConfirmationViewModelTest]/[ai.rojan.designlab.manager.presentation.calendar.ManagerAppointmentDetailViewModelTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RescheduleViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleBooking(id: String = "b1") = Booking(
        id = id,
        salonId = "s1",
        serviceId = "sv1",
        specialistId = "sp1",
        customerId = "c1",
        startTime = "2026-09-01T10:00:00",
        endTime = "2026-09-01T10:30:00",
        status = BookingStatus.CONFIRMED,
        notes = null,
    )

    /** Constructs a [RescheduleViewModel] that has already loaded (real [FakeBookingRepository.getBooking]/[FakeAvailabilityRepository] both resolve synchronously under [UnconfinedTestDispatcher]) and has a time selected, ready for [RescheduleViewModel.confirm]. */
    private fun readyViewModel(bookingRepository: FakeBookingRepository): RescheduleViewModel {
        val viewModel = RescheduleViewModel("b1", bookingRepository, FakeAvailabilityRepository())
        viewModel.selectTime("10:00")
        return viewModel
    }

    private fun readyState(viewModel: RescheduleViewModel): RescheduleUiState.Ready =
        viewModel.state as? RescheduleUiState.Ready ?: error("expected Ready state, was ${viewModel.state}")

    @Test
    fun `confirm success invokes rescheduleBooking once and fires onRescheduled`() = runTest {
        val bookingRepository = FakeBookingRepository(sampleBooking())
        val viewModel = readyViewModel(bookingRepository)
        var rescheduledCalled = false

        bookingRepository.rescheduleCompletion.complete(Result.success(sampleBooking()))
        viewModel.confirm(onRescheduled = { rescheduledCalled = true })

        assertTrue(rescheduledCalled)
        assertEquals(1, bookingRepository.rescheduleCallCount)
        // Today's real date, not hardcoded - RescheduleViewModel auto-selects RollingBookingDates'
        // real "today" as the initial selectedDate, same source of truth the ViewModel itself uses.
        val today = ai.rojan.designlab.domain.booking.RollingBookingDates.next7Days().first().first
        assertEquals("${today}T10:00:00", bookingRepository.lastNewStartTime)
    }

    @Test
    fun `confirm failure sets submitError and preserves the current selection`() = runTest {
        val bookingRepository = FakeBookingRepository(sampleBooking())
        val viewModel = readyViewModel(bookingRepository)

        bookingRepository.rescheduleCompletion.complete(Result.failure(IllegalStateException("conflict")))
        viewModel.confirm(onRescheduled = {})

        val state = readyState(viewModel)
        assertEquals("10:00", state.selectedTime)
        assertEquals(false, state.isSubmitting)
        assertTrue(state.submitError?.isNotBlank() == true)
    }

    @Test
    fun `calling confirm again while a request is in flight does not invoke rescheduleBooking twice`() = runTest {
        val bookingRepository = FakeBookingRepository(sampleBooking())
        val viewModel = readyViewModel(bookingRepository)
        var rescheduledCount = 0

        viewModel.confirm(onRescheduled = { rescheduledCount++ })

        assertEquals(1, bookingRepository.rescheduleCallCount)
        assertEquals(true, readyState(viewModel).isSubmitting)

        viewModel.confirm(onRescheduled = { rescheduledCount++ })

        // The guard must hold before the in-flight call resolves: still exactly 1 dispatch.
        assertEquals(1, bookingRepository.rescheduleCallCount)

        bookingRepository.rescheduleCompletion.complete(Result.success(sampleBooking()))

        // Exactly the one real reschedule completed - no second, silently-dropped duplicate fired.
        assertEquals(1, rescheduledCount)
    }

    private class FakeBookingRepository(private val initialBooking: Booking) : BookingRepository {
        val rescheduleCompletion = CompletableDeferred<Result<Booking>>()

        var rescheduleCallCount = 0
            private set
        var lastNewStartTime: String? = null
            private set

        override suspend fun createBooking(
            salonId: String,
            serviceId: String,
            specialistId: String,
            startTime: String,
            notes: String?,
            idempotencyKey: String?,
        ): Result<Booking> = error("not used")

        override suspend fun myBookings(page: Int, size: Int, status: BookingStatus?): Result<PagedResult<Booking>> =
            error("not used")

        override suspend fun getBooking(bookingId: String): Result<Booking> = Result.success(initialBooking)
        override suspend fun cancelBooking(bookingId: String): Result<Booking> = error("not used")
        override suspend fun confirmBooking(bookingId: String): Result<Booking> = error("not used")
        override suspend fun completeBooking(bookingId: String): Result<Booking> = error("not used")

        override suspend fun rescheduleBooking(bookingId: String, newStartTime: String): Result<Booking> {
            rescheduleCallCount++
            lastNewStartTime = newStartTime
            return rescheduleCompletion.await()
        }
    }

    private class FakeAvailabilityRepository(
        private val slots: List<TimeSlot> = listOf(TimeSlot(start = "2026-09-01T10:00:00", end = "2026-09-01T10:30:00")),
    ) : AvailabilityRepository {
        override suspend fun getAvailableSlots(
            salonId: String,
            specialistId: String,
            serviceId: String,
            date: String,
            slotIntervalMinutes: Int,
        ): Result<List<TimeSlot>> = Result.success(slots)
    }
}
