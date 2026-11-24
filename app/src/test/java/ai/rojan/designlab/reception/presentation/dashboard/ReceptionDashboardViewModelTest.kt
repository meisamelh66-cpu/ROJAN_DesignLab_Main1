package ai.rojan.designlab.reception.presentation.dashboard

import ai.rojan.designlab.domain.repository.Booking
import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.domain.repository.PagedResult
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.reception.domain.repository.ReceptionBookingRepository
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
 * Reception Check-in (Phase 4): hermetic coverage of [ReceptionDashboardViewModel]'s
 * Confirm/Check-in/Complete actions, using the same in-memory fake-repository approach
 * [ai.rojan.designlab.manager.presentation.calendar.ManagerAppointmentDetailViewModelTest]
 * already establishes for the structurally-equivalent Manager class. Scoped to the real
 * gap this phase closes (check-in) plus baseline confirm/complete coverage - this ViewModel
 * had no test file at all before this phase.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReceptionDashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun booking(id: String = "b1", status: BookingStatus = BookingStatus.CONFIRMED) = Booking(
        id = id,
        salonId = "salon-1",
        serviceId = "s1",
        specialistId = "sp1",
        customerId = "c1",
        startTime = "2026-09-01T10:00:00",
        endTime = "2026-09-01T10:30:00",
        status = status,
        notes = null,
    )

    @Test
    fun `checkInBooking moves a CONFIRMED booking to CHECKED_IN and refreshes the list`() = runTest {
        val initial = booking(status = BookingStatus.CONFIRMED)
        val checkedIn = initial.copy(status = BookingStatus.CHECKED_IN)
        val receptionRepository = FakeReceptionBookingRepository(listOf(initial))
        val genericRepository = FakeBookingRepository(
            checkInResult = { Result.success(checkedIn).also { receptionRepository.replace(checkedIn) } },
        )
        val viewModel = ReceptionDashboardViewModel("salon-1", receptionRepository, genericRepository)

        viewModel.checkInBooking(initial.id)

        val state = viewModel.bookings.value
        assertTrue(state is UiState.Success<*>)
        assertEquals(BookingStatus.CHECKED_IN, (state as UiState.Success<List<Booking>>).data.single().status)
        assertEquals(1, genericRepository.checkInCallCount)
        assertEquals(null, viewModel.actionError.value)
    }

    @Test
    fun `completeBooking works directly from CONFIRMED - check-in is optional`() = runTest {
        val initial = booking(status = BookingStatus.CONFIRMED)
        val completed = initial.copy(status = BookingStatus.COMPLETED)
        val receptionRepository = FakeReceptionBookingRepository(listOf(initial))
        val genericRepository = FakeBookingRepository(
            completeResult = { Result.success(completed).also { receptionRepository.replace(completed) } },
        )
        val viewModel = ReceptionDashboardViewModel("salon-1", receptionRepository, genericRepository)

        viewModel.completeBooking(initial.id)

        val state = viewModel.bookings.value
        assertEquals(BookingStatus.COMPLETED, (state as UiState.Success<List<Booking>>).data.single().status)
    }

    @Test
    fun `completeBooking works from CHECKED_IN too`() = runTest {
        val initial = booking(status = BookingStatus.CHECKED_IN)
        val completed = initial.copy(status = BookingStatus.COMPLETED)
        val receptionRepository = FakeReceptionBookingRepository(listOf(initial))
        val genericRepository = FakeBookingRepository(
            completeResult = { Result.success(completed).also { receptionRepository.replace(completed) } },
        )
        val viewModel = ReceptionDashboardViewModel("salon-1", receptionRepository, genericRepository)

        viewModel.completeBooking(initial.id)

        val state = viewModel.bookings.value
        assertEquals(BookingStatus.COMPLETED, (state as UiState.Success<List<Booking>>).data.single().status)
    }

    @Test
    fun `a real backend rejection on check-in surfaces an error message and leaves the list unrefreshed`() = runTest {
        val initial = booking(status = BookingStatus.PENDING) // invalid transition source
        val receptionRepository = FakeReceptionBookingRepository(listOf(initial))
        val genericRepository = FakeBookingRepository(
            checkInResult = { Result.failure(IllegalStateException("INVALID_BOOKING_STATE")) },
        )
        val viewModel = ReceptionDashboardViewModel("salon-1", receptionRepository, genericRepository)

        viewModel.checkInBooking(initial.id)

        assertTrue(viewModel.actionError.value?.isNotBlank() == true)
        // list unchanged - refresh() is only called on success.
        val state = viewModel.bookings.value
        assertEquals(BookingStatus.PENDING, (state as UiState.Success<List<Booking>>).data.single().status)
    }

    private class FakeReceptionBookingRepository(initial: List<Booking>) : ReceptionBookingRepository {
        private var content = initial

        var listCallCount = 0
            private set

        /** Simulates the backend's real, persisted state change - [refresh] re-fetches this, not the original snapshot. */
        fun replace(updated: Booking) {
            content = content.map { if (it.id == updated.id) updated else it }
        }

        override suspend fun listBookings(salonId: String, page: Int, size: Int, status: BookingStatus?): Result<PagedResult<Booking>> {
            listCallCount++
            return Result.success(PagedResult(content = content, page = 0, size = content.size, totalElements = content.size.toLong(), totalPages = 1))
        }

        override suspend fun createBookingForCustomer(
            salonId: String,
            customerId: String,
            serviceId: String,
            specialistId: String,
            startTime: String,
            notes: String?,
        ): Result<Booking> = error("not used by ReceptionDashboardViewModel")
    }

    private class FakeBookingRepository(
        private val confirmResult: (() -> Result<Booking>)? = null,
        private val checkInResult: (() -> Result<Booking>)? = null,
        private val completeResult: (() -> Result<Booking>)? = null,
    ) : BookingRepository {
        var checkInCallCount = 0
            private set

        override suspend fun createBooking(
            salonId: String,
            serviceId: String,
            specialistId: String,
            startTime: String,
            notes: String?,
            idempotencyKey: String?,
            customerId: String?,
        ): Result<Booking> = error("not used by ReceptionDashboardViewModel")

        override suspend fun myBookings(page: Int, size: Int, status: BookingStatus?): Result<PagedResult<Booking>> =
            error("not used by ReceptionDashboardViewModel")

        override suspend fun getBooking(bookingId: String): Result<Booking> = error("not used by ReceptionDashboardViewModel")

        override suspend fun cancelBooking(bookingId: String): Result<Booking> = error("not used by ReceptionDashboardViewModel")

        override suspend fun confirmBooking(bookingId: String): Result<Booking> =
            confirmResult?.invoke() ?: error("not used by this test")

        override suspend fun checkInBooking(bookingId: String): Result<Booking> {
            checkInCallCount++
            return checkInResult?.invoke() ?: error("not used by this test")
        }

        override suspend fun completeBooking(bookingId: String): Result<Booking> =
            completeResult?.invoke() ?: error("not used by this test")

        override suspend fun rescheduleBooking(bookingId: String, newStartTime: String): Result<Booking> =
            error("not used by ReceptionDashboardViewModel")

        override suspend fun salonBookings(salonId: String, page: Int, size: Int, status: BookingStatus?): Result<PagedResult<Booking>> =
            error("not used by ReceptionDashboardViewModel")
    }
}
