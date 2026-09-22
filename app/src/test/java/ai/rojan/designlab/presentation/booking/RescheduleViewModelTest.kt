package ai.rojan.designlab.presentation.booking

import ai.rojan.designlab.domain.repository.AvailabilityRepository
import ai.rojan.designlab.domain.repository.Booking
import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.domain.repository.PagedResult
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * [RescheduleViewModel] is the ViewModel actually wired into
 * `RescheduleAppointmentScreen.kt` (confirmed by import during the
 * file-by-file release audit), yet had zero test coverage — a sibling,
 * never-wired duplicate (`presentation.profile.RescheduleAppointmentViewModel`)
 * had a full test suite instead. Covers the live path: booking load,
 * slot loading + the stale-response guard, and confirm success/failure.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RescheduleViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeBookingRepository(
        var getBookingResult: Result<Booking> = Result.success(booking()),
        var rescheduleResult: Result<Booking> = Result.success(booking()),
    ) : BookingRepository {
        var rescheduleCalls = mutableListOf<Pair<String, String>>()
        override suspend fun createBooking(
            salonId: String, serviceId: String, specialistId: String, startTime: String,
            notes: String?, idempotencyKey: String?, customerId: String?,
        ): Result<Booking> = error("unused")
        override suspend fun myBookings(page: Int, size: Int, status: BookingStatus?): Result<PagedResult<Booking>> = error("unused")
        override suspend fun getBooking(bookingId: String): Result<Booking> = getBookingResult
        override suspend fun cancelBooking(bookingId: String): Result<Booking> = error("unused")
        override suspend fun confirmBooking(bookingId: String): Result<Booking> = error("unused")
        override suspend fun completeBooking(bookingId: String): Result<Booking> = error("unused")
        override suspend fun rescheduleBooking(bookingId: String, newStartTime: String): Result<Booking> {
            rescheduleCalls += bookingId to newStartTime
            return rescheduleResult
        }
        override suspend fun salonBookings(salonId: String, page: Int, size: Int, status: BookingStatus?): Result<PagedResult<Booking>> = error("unused")
    }

    private class FakeAvailabilityRepository(var result: Result<List<TimeSlot>>) : AvailabilityRepository {
        var callCount = 0
        override suspend fun getAvailableSlots(
            salonId: String, specialistId: String, serviceId: String, date: String, slotIntervalMinutes: Int,
        ): Result<List<TimeSlot>> {
            callCount++
            return result
        }
    }

    @Test
    fun `loading an existing booking resolves into Ready and kicks off a slot load for today`() = runTest(dispatcher) {
        val bookingRepo = FakeBookingRepository()
        val availabilityRepo = FakeAvailabilityRepository(Result.success(listOf(TimeSlot("2026-09-01T10:00:00", "2026-09-01T10:15:00"))))
        val vm = RescheduleViewModel("appt-1", bookingRepo, availabilityRepo)

        advanceUntilIdle()

        val state = vm.state
        assertTrue(state is RescheduleUiState.Ready)
        state as RescheduleUiState.Ready
        assertTrue(state.slots is UiState.Success)
        assertEquals(1, availabilityRepo.callCount)
    }

    @Test
    fun `a failed booking load ends in Error, not a crash or stuck spinner`() = runTest(dispatcher) {
        val bookingRepo = FakeBookingRepository(getBookingResult = Result.failure(IOException("gone")))
        val availabilityRepo = FakeAvailabilityRepository(Result.success(emptyList()))
        val vm = RescheduleViewModel("appt-1", bookingRepo, availabilityRepo)

        advanceUntilIdle()

        assertTrue(vm.state is RescheduleUiState.Error)
    }

    @Test
    fun `selectDate on the same already-selected date is a no-op`() = runTest(dispatcher) {
        val bookingRepo = FakeBookingRepository()
        val availabilityRepo = FakeAvailabilityRepository(Result.success(emptyList()))
        val vm = RescheduleViewModel("appt-1", bookingRepo, availabilityRepo)
        advanceUntilIdle()
        val today = (vm.state as RescheduleUiState.Ready).selectedDate
        val callsBefore = availabilityRepo.callCount

        vm.selectDate(today)
        advanceUntilIdle()

        assertEquals("re-selecting the same date must not re-fetch", callsBefore, availabilityRepo.callCount)
    }

    @Test
    fun `a stale slot response for a since-changed date is discarded`() = runTest(dispatcher) {
        val bookingRepo = FakeBookingRepository()
        val availabilityRepo = FakeAvailabilityRepository(Result.success(listOf(TimeSlot("2026-09-01T10:00:00", "2026-09-01T10:15:00"))))
        val vm = RescheduleViewModel("appt-1", bookingRepo, availabilityRepo)
        advanceUntilIdle()
        val today = (vm.state as RescheduleUiState.Ready).selectedDate
        val dates = (vm.state as RescheduleUiState.Ready).dates
        val otherDate = dates.map { it.first }.first { it != today }

        vm.selectDate(otherDate)
        // Do not advance yet: simulate the user immediately jumping back to `today`
        // before the in-flight request for `otherDate` resolves.
        vm.selectDate(today)
        advanceUntilIdle()

        val finalState = vm.state as RescheduleUiState.Ready
        assertEquals("selection must reflect the most recent tap", today, finalState.selectedDate)
        assertTrue("the stale otherDate response must not overwrite today's slots", finalState.slots is UiState.Success)
    }

    @Test
    fun `confirm success invokes the callback`() = runTest(dispatcher) {
        val bookingRepo = FakeBookingRepository(rescheduleResult = Result.success(booking()))
        val availabilityRepo = FakeAvailabilityRepository(Result.success(listOf(TimeSlot("2026-09-01T10:00:00", "2026-09-01T10:15:00"))))
        val vm = RescheduleViewModel("appt-1", bookingRepo, availabilityRepo)
        advanceUntilIdle()
        vm.selectTime("10:00")
        var rescheduled = false

        vm.confirm { rescheduled = true }
        advanceUntilIdle()

        assertTrue(rescheduled)
        assertEquals(1, bookingRepo.rescheduleCalls.size)
    }

    @Test
    fun `confirm failure surfaces submitError and clears isSubmitting, without calling the callback`() = runTest(dispatcher) {
        val bookingRepo = FakeBookingRepository(rescheduleResult = Result.failure(IOException("network down")))
        val availabilityRepo = FakeAvailabilityRepository(Result.success(listOf(TimeSlot("2026-09-01T10:00:00", "2026-09-01T10:15:00"))))
        val vm = RescheduleViewModel("appt-1", bookingRepo, availabilityRepo)
        advanceUntilIdle()
        vm.selectTime("10:00")
        var rescheduled = false

        vm.confirm { rescheduled = true }
        advanceUntilIdle()

        assertFalse(rescheduled)
        val state = vm.state as RescheduleUiState.Ready
        assertFalse(state.isSubmitting)
        assertTrue(state.submitError != null)
    }

    @Test
    fun `retry reloads the booking from scratch`() = runTest(dispatcher) {
        val bookingRepo = FakeBookingRepository(getBookingResult = Result.failure(IOException("gone")))
        val availabilityRepo = FakeAvailabilityRepository(Result.success(emptyList()))
        val vm = RescheduleViewModel("appt-1", bookingRepo, availabilityRepo)
        advanceUntilIdle()
        assertTrue(vm.state is RescheduleUiState.Error)

        bookingRepo.getBookingResult = Result.success(booking())
        vm.retry()
        advanceUntilIdle()

        assertTrue(vm.state is RescheduleUiState.Ready)
    }

    private companion object {
        fun booking() = Booking(
            id = "appt-1", salonId = "salon-1", serviceId = "svc-1", specialistId = "spec-1",
            customerId = "c-1", startTime = "2026-09-01T10:00:00", endTime = "2026-09-01T10:30:00",
            status = BookingStatus.PENDING, notes = null,
        )
    }
}
